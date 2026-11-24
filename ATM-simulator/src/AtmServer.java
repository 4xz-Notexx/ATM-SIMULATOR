import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * AtmServer
 * ---------
 * A lightweight HTTP layer that exposes the EXISTING ATM backend to a web
 * frontend. It does NOT modify any existing backend class; it reuses them
 * directly, exactly as the console ATM does:
 *
 *   - User.findAccount()              -> Linear Search (login + DSA demo)
 *   - Account.validatePin()           -> PIN check
 *   - Account.deposit() / withdraw()  -> balance management
 *   - CashDispenser.getDispensePlan() / dispense() -> Greedy algorithm
 *   - Account.getTransactionHistory() (TransactionStack) -> LIFO history
 *   - TransactionQueue                -> FIFO pending requests
 *
 * It also serves the static frontend files from ./frontend so the whole app
 * runs on one origin (no CORS issues).
 *
 * Single-session model (mirrors the console app): one authenticated account at
 * a time. Requests run on a single thread so the shared state stays consistent.
 *
 * Run from the project root:  java -cp bin ServerMain
 */
public class AtmServer {

    private static final String FRONTEND_DIR = "frontend";
    private static final int DEFAULT_PORT = 8080;

    private final Account[] accounts;
    private final User user;                 // reused for Linear Search (findAccount)
    private final CashDispenser dispenser;
    private final TransactionQueue pendingQueue;

    // Single-session state (mirrors the console app's User.currentAccount).
    private Account currentAccount;
    // Consecutive failed PIN attempts per account number (3-try lockout).
    private final Map<Integer, Integer> failedAttempts = new HashMap<>();

    private final HttpServer server;

    public AtmServer(int port) throws IOException {
        // Same sample (dummy, educational) accounts as the console ATM.
        accounts = new Account[] {
            new Account(1001, 1234, "Anas Ahmad",   5000),
            new Account(1002, 2345, "Rahul Sharma", 10000),
            new Account(1003, 3456, "Aman Kumar",   7500)
        };
        user = new User(accounts);
        dispenser = new CashDispenser();
        pendingQueue = new TransactionQueue();
        currentAccount = null;

        server = HttpServer.create(new InetSocketAddress(port), 0);
        registerRoutes();
        server.setExecutor(Executors.newSingleThreadExecutor());
    }

    public void start() {
        server.start();
        System.out.println("ATM web server running on port " + getPort() + ".");
        System.out.println("Open  http://localhost:" + getPort() + "  in your browser.");
        System.out.println("Press Ctrl+C to stop.");
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    public void stop() {
        server.stop(0);
    }

    private void registerRoutes() {
        // API
        server.createContext("/api/login", this::handleLogin);
        server.createContext("/api/logout", this::handleLogout);
        server.createContext("/api/session", this::handleSession);
        server.createContext("/api/balance", this::handleBalance);
        server.createContext("/api/withdraw", this::handleWithdraw);
        server.createContext("/api/deposit", this::handleDeposit);
        server.createContext("/api/history", this::handleHistory);
        server.createContext("/api/reverse", this::handleReverse);
        server.createContext("/api/pending/add", this::handlePendingAdd);
        server.createContext("/api/pending/process", this::handlePendingProcess);
        server.createContext("/api/pending/list", this::handlePendingList);
        server.createContext("/api/dsa/stack", this::handleDsaStack);
        server.createContext("/api/dsa/queue", this::handleDsaQueue);
        server.createContext("/api/dsa/search", this::handleDsaSearch);
        server.createContext("/api/dsa/greedy", this::handleDsaGreedy);
        // Static frontend (catch-all)
        server.createContext("/", this::handleStatic);
    }

    // ---------------- helpers ----------------

    private boolean preflight(HttpExchange ex) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            ex.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private void send(HttpExchange ex, int status, String body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] bytes = body.getBytes("UTF-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private Map<String, String> params(HttpExchange ex) {
        Map<String, String> map = new HashMap<>();
        String query = ex.getRequestURI().getQuery();
        if (query == null || query.isEmpty()) {
            return map;
        }
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            String k, v;
            if (idx < 0) { k = pair; v = ""; }
            else { k = pair.substring(0, idx); v = pair.substring(idx + 1); }
            try {
                map.put(URLDecoder.decode(k, "UTF-8"), URLDecoder.decode(v, "UTF-8"));
            } catch (Exception e) {
                map.put(k, v);
            }
        }
        return map;
    }

    private int paramInt(Map<String, String> p, String key, int def) {
        String v = p.get(key);
        if (v == null || v.isEmpty()) {
            return def;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** Snapshot the stack top->bottom WITHOUT destroying it (pop all, then re-push). */
    private Transaction[] snapshotStack(TransactionStack s) {
        int n = s.size();
        Transaction[] arr = new Transaction[n];
        for (int i = 0; i < n; i++) {
            arr[i] = s.pop();          // arr[0] = top (newest)
        }
        for (int i = n - 1; i >= 0; i--) {
            s.push(arr[i]);             // restore exact order
        }
        return arr;                    // top-first (newest first)
    }

    /** Snapshot the queue front->rear WITHOUT destroying it (dequeue all, re-enqueue). */
    private Transaction[] snapshotQueue(TransactionQueue q) {
        int n = q.size();
        Transaction[] arr = new Transaction[n];
        for (int i = 0; i < n; i++) {
            arr[i] = q.dequeue();       // arr[0] = front (oldest)
        }
        for (int i = 0; i < n; i++) {
            q.enqueue(arr[i]);          // restore exact FIFO order
        }
        return arr;                    // front-first
    }

    private String txnJson(Transaction t) {
        return "{"
            + "\"id\":" + t.getTransactionId() + ","
            + "\"type\":\"" + Json.esc(t.getType()) + "\","
            + "\"amount\":" + Json.num(t.getAmount()) + ","
            + "\"timestamp\":\"" + Json.esc(t.getFormattedTimestamp()) + "\","
            + "\"reversed\":" + t.isReversed()
            + "}";
    }

    private String txnArrayJson(Transaction[] items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(txnJson(items[i]));
        }
        return sb.append("]").toString();
    }

    private String breakdownJson(int[] notes) {
        int[] denoms = dispenser.getDenominations();
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < denoms.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(denoms[i]).append("\":").append(notes[i]);
        }
        return sb.append("}").toString();
    }

    // ---------------- API handlers ----------------

    private void handleLogin(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;
        Map<String, String> p = params(ex);
        int accNum = paramInt(p, "accountNumber", -1);
        int pin = paramInt(p, "pin", -1);
        if (accNum < 0 || pin < 0) {
            send(ex, 400, "{\"success\":false,\"message\":\"Account number and PIN are required.\"}");
            return;
        }
        Account acc = user.findAccount(accNum);               // Linear Search
        if (acc == null) {
            send(ex, 200, "{\"success\":false,\"message\":\"Account not found.\"}");
            return;
        }
        if (acc.validatePin(pin)) {
            failedAttempts.remove(accNum);
            currentAccount = acc;
            send(ex, 200, "{\"success\":true,\"accountNumber\":" + accNum
                + ",\"name\":\"" + Json.esc(acc.getHolderName()) + "\""
                + ",\"balance\":" + Json.num(acc.getBalance()) + "}");
        } else {
            int attempts = failedAttempts.getOrDefault(accNum, 0) + 1;
            if (attempts >= 3) {
                failedAttempts.put(accNum, 0);                 // reset, like a fresh login() call
                send(ex, 200, "{\"success\":false,\"message\":\"Maximum PIN attempts reached. Please retry.\",\"attemptsLeft\":0}");
            } else {
                failedAttempts.put(accNum, attempts);
                send(ex, 200, "{\"success\":false,\"message\":\"Incorrect PIN.\",\"attemptsLeft\":" + (3 - attempts) + "}");
            }
        }
    }

    private void handleLogout(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;
        currentAccount = null;
        send(ex, 200, "{\"success\":true}");
    }

    private void handleSession(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;
        if (currentAccount == null) {
            send(ex, 200, "{\"authenticated\":false}");
            return;
        }
        send(ex, 200, "{\"authenticated\":true,\"accountNumber\":" + currentAccount.getAccountNumber()
            + ",\"name\":\"" + Json.esc(currentAccount.getHolderName()) + "\""
            + ",\"balance\":" + Json.num(currentAccount.getBalance()) + "}");
    }

    private void handleBalance(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;
        if (currentAccount == null) {
            send(ex, 401, "{\"success\":false,\"message\":\"Not authenticated.\"}");
            return;
        }
        send(ex, 200, "{\"success\":true,\"accountNumber\":" + currentAccount.getAccountNumber()
            + ",\"balance\":" + Json.num(currentAccount.getBalance()) + "}");
    }

    private void handleWithdraw(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;
        if (currentAccount == null) {
            send(ex, 401, "{\"success\":false,\"message\":\"Not authenticated.\"}");
            return;
        }
        int amount = paramInt(params(ex), "amount", -1);
        if (amount <= 0) {
            send(ex, 200, "{\"success\":false,\"message\":\"Invalid amount. Please enter a positive value.\"}");
            return;
        }
        if (amount % 10 != 0) {
            send(ex, 200, "{\"success\":false,\"message\":\"Amount must be in multiples of 10.\"}");
            return;
        }
        if (amount > currentAccount.getBalance()) {
            send(ex, 200, "{\"success\":false,\"message\":\"Insufficient balance.\",\"balance\":"
                + Json.num(currentAccount.getBalance()) + "}");
            return;
        }
        if (!dispenser.canDispense(amount)) {
            send(ex, 200, "{\"success\":false,\"message\":\"ATM cannot dispense this amount. Insufficient notes.\",\"availableCash\":"
                + dispenser.totalCashAvailable() + "}");
            return;
        }
        int[] notes = dispenser.dispense(amount);
        currentAccount.withdraw(amount);
        Transaction t = new Transaction("WITHDRAW", amount, true);
        t.setDispensedNotes(notes);
        currentAccount.getTransactionHistory().push(t);
        send(ex, 200, "{\"success\":true,\"dispensed\":" + amount
            + ",\"balance\":" + Json.num(currentAccount.getBalance())
            + ",\"breakdown\":" + breakdownJson(notes) + "}");
    }

    private void handleDeposit(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;
        if (currentAccount == null) {
            send(ex, 401, "{\"success\":false,\"message\":\"Not authenticated.\"}");
            return;
        }
        int amount = paramInt(params(ex), "amount", -1);
        if (amount <= 0) {
            send(ex, 200, "{\"success\":false,\"message\":\"Invalid amount. Please enter a positive value.\"}");
            return;
        }
        currentAccount.deposit(amount);
        currentAccount.getTransactionHistory().push(new Transaction("DEPOSIT", amount, true));
        send(ex, 200, "{\"success\":true,\"balance\":" + Json.num(currentAccount.getBalance()) + "}");
    }

    private void handleHistory(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;
        if (currentAccount == null) {
            send(ex, 401, "{\"success\":false,\"message\":\"Not authenticated.\"}");
            return;
        }
        Transaction[] items = snapshotStack(currentAccount.getTransactionHistory());
        send(ex, 200, "{\"success\":true,\"transactions\":" + txnArrayJson(items) + "}");
    }

    private void handleReverse(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;
        if (currentAccount == null) {
            send(ex, 401, "{\"success\":false,\"message\":\"Not authenticated.\"}");
            return;
        }
        TransactionStack history = currentAccount.getTransactionHistory();
        if (history.isEmpty()) {
            send(ex, 200, "{\"success\":false,\"message\":\"No transactions to reverse.\"}");
            return;
        }
        Transaction latest = history.peek();
        if (!latest.canReverse()) {
            send(ex, 200, "{\"success\":false,\"message\":\"The most recent transaction is not eligible for reversal.\"}");
            return;
        }
        String type = latest.getType();
        if (type.equals("WITHDRAW")) {
            currentAccount.deposit(latest.getAmount());
            if (latest.getDispensedNotes() != null) {
                dispenser.returnNotes(latest.getDispensedNotes());
            }
        } else if (type.equals("DEPOSIT")) {
            if (latest.getAmount() > currentAccount.getBalance()) {
                send(ex, 200, "{\"success\":false,\"message\":\"Cannot reverse: insufficient balance.\"}");
                return;
            }
            currentAccount.withdraw(latest.getAmount());
        } else {
            send(ex, 200, "{\"success\":false,\"message\":\"This transaction type cannot be reversed.\"}");
            return;
        }
        latest.markReversed();
        history.push(new Transaction("REVERSAL of " + type + " #" + latest.getTransactionId(),
                latest.getAmount(), false));
        send(ex, 200, "{\"success\":true,\"reversedType\":\"" + Json.esc(type) + "\""
            + ",\"reversedAmount\":" + Json.num(latest.getAmount())
            + ",\"balance\":" + Json.num(currentAccount.getBalance()) + "}");
    }

    private void handlePendingAdd(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;
        if (currentAccount == null) {
            send(ex, 401, "{\"success\":false,\"message\":\"Not authenticated.\"}");
            return;
        }
        Map<String, String> p = params(ex);
        String type = p.get("type");
        int amount = paramInt(p, "amount", -1);
        if (type == null || (!type.equals("PENDING_WITHDRAW") && !type.equals("PENDING_DEPOSIT"))) {
            send(ex, 200, "{\"success\":false,\"message\":\"Invalid request type.\"}");
            return;
        }
        if (amount <= 0) {
            send(ex, 200, "{\"success\":false,\"message\":\"Invalid amount.\"}");
            return;
        }
        pendingQueue.enqueue(new Transaction(type, amount, false));
        send(ex, 200, "{\"success\":true,\"size\":" + pendingQueue.size() + "}");
    }

    private void handlePendingProcess(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;
        if (currentAccount == null) {
            send(ex, 401, "{\"success\":false,\"message\":\"Not authenticated.\"}");
            return;
        }
        if (pendingQueue.isEmpty()) {
            send(ex, 200, "{\"success\":false,\"message\":\"No pending requests to process.\"}");
            return;
        }
        Transaction req = pendingQueue.dequeue();
        if (req.getType().equals("PENDING_WITHDRAW")) {
            int amount = (int) req.getAmount();
            if (amount <= 0 || amount % 10 != 0) {
                send(ex, 200, "{\"success\":false,\"message\":\"Request failed: amount must be a positive multiple of 10. Discarded.\"}");
                return;
            }
            if (amount > currentAccount.getBalance()) {
                send(ex, 200, "{\"success\":false,\"message\":\"Request failed: insufficient balance. Discarded.\"}");
                return;
            }
            if (!dispenser.canDispense(amount)) {
                send(ex, 200, "{\"success\":false,\"message\":\"Request failed: ATM cannot dispense. Discarded.\"}");
                return;
            }
            int[] notes = dispenser.dispense(amount);
            currentAccount.withdraw(amount);
            Transaction t = new Transaction("WITHDRAW", amount, true);
            t.setDispensedNotes(notes);
            currentAccount.getTransactionHistory().push(t);
            send(ex, 200, "{\"success\":true,\"kind\":\"WITHDRAW\",\"amount\":" + amount
                + ",\"balance\":" + Json.num(currentAccount.getBalance())
                + ",\"breakdown\":" + breakdownJson(notes) + "}");
        } else {
            currentAccount.deposit(req.getAmount());
            currentAccount.getTransactionHistory().push(new Transaction("DEPOSIT", req.getAmount(), true));
            send(ex, 200, "{\"success\":true,\"kind\":\"DEPOSIT\",\"amount\":" + Json.num(req.getAmount())
                + ",\"balance\":" + Json.num(currentAccount.getBalance()) + "}");
        }
    }

    private void handlePendingList(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;
        if (currentAccount == null) {
            send(ex, 401, "{\"success\":false,\"message\":\"Not authenticated.\"}");
            return;
        }
        Transaction[] items = snapshotQueue(pendingQueue);
        send(ex, 200, "{\"success\":true,\"size\":" + items.length + ",\"requests\":" + txnArrayJson(items) + "}");
    }

    private void handleDsaStack(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;
        if (currentAccount == null) {
            send(ex, 401, "{\"success\":false,\"message\":\"Not authenticated.\"}");
            return;
        }
        Transaction[] items = snapshotStack(currentAccount.getTransactionHistory());
        send(ex, 200, "{\"success\":true,\"size\":" + items.length + ",\"items\":" + txnArrayJson(items) + "}");
    }

    private void handleDsaQueue(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;
        Transaction[] items = snapshotQueue(pendingQueue);
        send(ex, 200, "{\"success\":true,\"size\":" + items.length + ",\"items\":" + txnArrayJson(items) + "}");
    }

    private void handleDsaSearch(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;
        int accNum = paramInt(params(ex), "accountNumber", -1);
        if (accNum < 0) {
            send(ex, 200, "{\"found\":false,\"message\":\"Enter an account number.\"}");
            return;
        }
        // Use the backend's Linear Search (User.findAccount) to locate the account.
        Account acc = user.findAccount(accNum);
        if (acc == null) {
            send(ex, 200, "{\"found\":false,\"accountNumber\":" + accNum
                + ",\"scanned\":" + accounts.length + "}");
            return;
        }
        // Find the index via a linear scan (for visualisation).
        int index = -1;
        for (int i = 0; i < accounts.length; i++) {
            if (accounts[i].getAccountNumber() == accNum) {
                index = i;
                break;
            }
        }
        send(ex, 200, "{\"found\":true,\"index\":" + index
            + ",\"accountNumber\":" + acc.getAccountNumber()
            + ",\"name\":\"" + Json.esc(acc.getHolderName()) + "\""
            + ",\"balance\":" + Json.num(acc.getBalance())
            + ",\"scanned\":" + (index + 1) + "}");
    }

    private void handleDsaGreedy(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;
        int amount = paramInt(params(ex), "amount", -1);
        if (amount <= 0) {
            send(ex, 200, "{\"success\":false,\"message\":\"Enter a positive amount.\"}");
            return;
        }
        if (amount % 10 != 0) {
            send(ex, 200, "{\"success\":false,\"message\":\"Amount must be a multiple of 10.\"}");
            return;
        }
        int[] plan = dispenser.getDispensePlan(amount);  // dry run: does NOT change state
        if (plan == null) {
            send(ex, 200, "{\"success\":false,\"message\":\"Cannot dispense exactly with available notes.\",\"amount\":"
                + amount + ",\"availableCash\":" + dispenser.totalCashAvailable() + "}");
            return;
        }
        int noteCount = 0;
        for (int n : plan) noteCount += n;
        send(ex, 200, "{\"success\":true,\"amount\":" + amount
            + ",\"breakdown\":" + breakdownJson(plan)
            + ",\"noteCount\":" + noteCount
            + ",\"availableCash\":" + dispenser.totalCashAvailable() + "}");
    }

    // ---------------- static frontend ----------------

    private void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }
        if (path.contains("..")) {
            sendStatic(ex, 400, "Bad request".getBytes("UTF-8"), "text/plain; charset=utf-8");
            return;
        }
        String rel = path.startsWith("/") ? path.substring(1) : path;
        Path base = Paths.get(FRONTEND_DIR).normalize();
        Path file = Paths.get(FRONTEND_DIR, rel).normalize();
        if (!file.startsWith(base) || !Files.exists(file) || Files.isDirectory(file)) {
            sendStatic(ex, 404, "Not found".getBytes("UTF-8"), "text/plain; charset=utf-8");
            return;
        }
        byte[] data = Files.readAllBytes(file);
        sendStatic(ex, 200, data, contentType(path));
    }

    private void sendStatic(HttpExchange ex, int status, byte[] data, String contentType) throws IOException {
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.getResponseHeaders().set("Cache-Control", "no-cache");
        ex.sendResponseHeaders(status, data.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(data);
        }
    }

    private String contentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".css"))  return "text/css; charset=utf-8";
        if (path.endsWith(".js"))   return "application/javascript; charset=utf-8";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".png"))  return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".ico"))  return "image/x-icon";
        return "application/octet-stream";
    }
}
