import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * ATM
 * ---
 * The central controller of the application. It owns:
 *   - an array of Accounts            (DSA: Arrays)
 *   - a CashDispenser                 (DSA: Greedy Algorithm)
 *   - a TransactionQueue of requests  (DSA: Queue / FIFO)
 *   - a User for authentication       (DSA: Linear Search)
 *
 * It runs the main application loop: login -> menu -> operations -> logout/exit.
 */
public class ATM {

    private final Account[] accounts;
    private final CashDispenser dispenser;
    private final TransactionQueue pendingQueue;
    private final User user;
    private final Scanner scanner;
    private boolean running;

    public ATM() {
        // Sample (dummy, educational) accounts stored in an array.
        accounts = new Account[] {
            new Account(1001, 1234, "Anas Ahmad",   5000),
            new Account(1002, 2345, "Rahul Sharma", 10000),
            new Account(1003, 3456, "Aman Kumar",   7500)
        };
        dispenser = new CashDispenser();
        pendingQueue = new TransactionQueue();
        user = new User(accounts);
        scanner = new Scanner(System.in);
        running = true;
    }

    /** Entry point of the ATM application loop. */
    public void start() {
        printBanner();
        while (running) {
            try {
                if (!user.isAuthenticated()) {
                    user.login(scanner);
                    continue;
                }
                showMenu();
                int choice = readInt("Enter your choice: ");
                handleChoice(choice);
            } catch (NoSuchElementException e) {
                // The input stream was closed (e.g. Ctrl+D / Ctrl+Z). Exit gracefully
                // instead of crashing or looping forever on empty input.
                System.out.println("\nInput stream ended. Shutting down safely.");
                running = false;
            }
        }
        scanner.close();
    }

    private void printBanner() {
        System.out.println("====================================");
        System.out.println("          ATM SIMULATOR");
        System.out.println("====================================");
    }

    private void showMenu() {
        Account acc = user.getCurrentAccount();
        System.out.println("--------------- ATM MENU ---------------");
        System.out.println("Account: " + acc.getAccountNumber() + "  |  Holder: " + acc.getHolderName());
        System.out.println("1. Check Balance");
        System.out.println("2. Withdraw Cash");
        System.out.println("3. Deposit Cash");
        System.out.println("4. Transaction History");
        System.out.println("5. Pending Transaction Requests");
        System.out.println("6. Reverse Last Eligible Transaction");
        System.out.println("7. Logout");
        System.out.println("8. Exit");
        System.out.println("---------------------------------------");
    }

    private void handleChoice(int choice) {
        if (choice == -1) {
            return;   // invalid numeric input was already reported by readInt
        }
        switch (choice) {
            case 1: checkBalance();            break;
            case 2: withdraw();                break;
            case 3: deposit();                 break;
            case 4: showTransactionHistory();  break;
            case 5: pendingRequests();         break;
            case 6: reverseTransaction();      break;
            case 7: logout();                  break;
            case 8: exit();                    break;
            default: System.out.println("Invalid choice. Please select an option from 1 to 8.");
        }
    }

    // ---------------- Operations ----------------

    private void checkBalance() {
        Account acc = user.getCurrentAccount();
        System.out.println("\n--- Balance Inquiry ---");
        System.out.printf("Current Balance: Rs. %.2f%n%n", acc.getBalance());
    }

    private void withdraw() {
        Account acc = user.getCurrentAccount();
        System.out.println("\n--- Cash Withdrawal ---");
        int amount = readInt("Enter withdrawal amount (multiples of 10): ");

        if (amount <= 0) {
            System.out.println("Invalid amount. Please enter a positive value.");
            return;
        }
        if (amount % 10 != 0) {
            System.out.println("Amount must be in multiples of 10 (the smallest note is Rs. 10).");
            return;
        }
        if (amount > acc.getBalance()) {
            System.out.printf("Insufficient balance. Your balance is Rs. %.2f.%n", acc.getBalance());
            return;
        }
        if (!dispenser.canDispense(amount)) {
            System.out.println("ATM cannot dispense this amount. Insufficient notes in the ATM.");
            System.out.println("Total cash available in ATM: Rs. " + dispenser.totalCashAvailable());
            return;
        }

        // Only now do we actually deduct notes and money.
        int[] notes = dispenser.dispense(amount);
        acc.withdraw(amount);
        // Keep the note breakdown with the transaction so that a later reversal can
        // return the exact notes to the ATM and keep its inventory consistent.
        Transaction txn = new Transaction("WITHDRAW", amount, true);
        txn.setDispensedNotes(notes);
        acc.getTransactionHistory().push(txn);

        System.out.println("Withdrawal successful!");
        System.out.println("Denomination breakdown:");
        int[] denoms = dispenser.getDenominations();
        for (int i = 0; i < denoms.length; i++) {
            if (notes[i] > 0) {
                System.out.printf("  Rs. %3d x %d = Rs. %d%n", denoms[i], notes[i], denoms[i] * notes[i]);
            }
        }
        System.out.printf("Total dispensed: Rs. %d%n", amount);
        System.out.printf("Remaining balance: Rs. %.2f%n%n", acc.getBalance());
    }

    private void deposit() {
        Account acc = user.getCurrentAccount();
        System.out.println("\n--- Cash Deposit ---");
        int amount = readInt("Enter deposit amount: ");

        if (amount <= 0) {
            System.out.println("Invalid amount. Please enter a positive value.");
            return;
        }

        acc.deposit(amount);
        acc.getTransactionHistory().push(new Transaction("DEPOSIT", amount, true));

        System.out.println("Deposit successful!");
        System.out.printf("Deposited: Rs. %d%n", amount);
        System.out.printf("New balance: Rs. %.2f%n%n", acc.getBalance());
    }

    private void showTransactionHistory() {
        Account acc = user.getCurrentAccount();
        System.out.println("\n--- Transaction History (newest first) ---");
        TransactionStack history = acc.getTransactionHistory();
        if (history.isEmpty()) {
            System.out.println("  No transactions yet.");
        } else {
            history.display();   // Stack LIFO traversal: latest first
        }
        System.out.println();
    }

    private void reverseTransaction() {
        Account acc = user.getCurrentAccount();
        System.out.println("\n--- Reverse Last Eligible Transaction ---");
        TransactionStack history = acc.getTransactionHistory();

        if (history.isEmpty()) {
            System.out.println("No transactions to reverse.");
            return;
        }

        Transaction latest = history.peek();
        if (!latest.canReverse()) {
            System.out.println("The most recent transaction is not eligible for reversal.");
            System.out.println("Latest: " + latest);
            return;
        }

        String type = latest.getType();
        if (type.equals("WITHDRAW")) {
            // Reversing a withdrawal: give the money back to the account AND return
            // the physical notes to the ATM so its note inventory stays consistent.
            acc.deposit(latest.getAmount());
            if (latest.getDispensedNotes() != null) {
                dispenser.returnNotes(latest.getDispensedNotes());
            }
        } else if (type.equals("DEPOSIT")) {
            // Reversing a deposit: take the money back out (only if balance allows).
            if (latest.getAmount() > acc.getBalance()) {
                System.out.println("Cannot reverse: insufficient balance to reverse this deposit.");
                return;
            }
            acc.withdraw(latest.getAmount());
        } else {
            System.out.println("This transaction type cannot be reversed.");
            return;
        }

        // Mark the original as reversed so it can never be reversed again.
        latest.markReversed();
        // Record a non-reversible REVERSAL entry on top of the stack.
        history.push(new Transaction("REVERSAL of " + type + " #" + latest.getTransactionId(),
                latest.getAmount(), false));

        System.out.println("Transaction reversed successfully!");
        System.out.printf("Reversed amount: Rs. %.2f%n", latest.getAmount());
        System.out.printf("New balance: Rs. %.2f%n%n", acc.getBalance());
    }

    // ---------------- Pending Requests (Queue) ----------------

    private void pendingRequests() {
        while (running) {
            System.out.println("\n--- Pending Transaction Requests (Queue) ---");
            System.out.println("1. Add Withdrawal Request");
            System.out.println("2. Add Deposit Request");
            System.out.println("3. Process Next Request (FIFO)");
            System.out.println("4. View All Pending Requests");
            System.out.println("5. Back to Main Menu");
            int sub = readInt("Enter your choice: ");
            switch (sub) {
                case 1: addPendingRequest("PENDING_WITHDRAW"); break;
                case 2: addPendingRequest("PENDING_DEPOSIT");  break;
                case 3: processNextRequest();                  break;
                case 4: viewPendingRequests();                 break;
                case 5: return;
                default:
                    if (sub != -1) {
                        System.out.println("Invalid choice. Please select 1 to 5.");
                    }
            }
        }
    }

    private void addPendingRequest(String type) {
        int amount = readInt("Enter amount: ");
        if (amount <= 0) {
            System.out.println("Invalid amount. Please enter a positive value.");
            return;
        }
        pendingQueue.enqueue(new Transaction(type, amount, false));
        System.out.println("Request added to the pending queue.");
        System.out.println("Pending requests in queue: " + pendingQueue.size());
    }

    private void processNextRequest() {
        if (pendingQueue.isEmpty()) {
            System.out.println("No pending requests to process.");
            return;
        }
        Transaction req = pendingQueue.dequeue();   // FIFO: oldest first
        Account acc = user.getCurrentAccount();
        System.out.println("Processing request: " + req);

        if (req.getType().equals("PENDING_WITHDRAW")) {
            // A pending withdrawal is processed exactly like a normal one: the
            // amount must be a positive multiple of 10 and dispensable from the ATM.
            int amount = (int) req.getAmount();
            if (amount <= 0 || amount % 10 != 0) {
                System.out.println("Request failed: amount must be a positive multiple of 10. Request discarded.");
                return;
            }
            if (amount > acc.getBalance()) {
                System.out.println("Request failed: insufficient balance. Request discarded.");
                return;
            }
            if (!dispenser.canDispense(amount)) {
                System.out.println("Request failed: ATM cannot dispense this amount. Request discarded.");
                return;
            }
            int[] notes = dispenser.dispense(amount);
            acc.withdraw(amount);
            Transaction txn = new Transaction("WITHDRAW", amount, true);
            txn.setDispensedNotes(notes);
            acc.getTransactionHistory().push(txn);

            System.out.println("Withdrawal request processed!");
            System.out.println("Denomination breakdown:");
            int[] denoms = dispenser.getDenominations();
            for (int i = 0; i < denoms.length; i++) {
                if (notes[i] > 0) {
                    System.out.printf("  Rs. %3d x %d = Rs. %d%n", denoms[i], notes[i], denoms[i] * notes[i]);
                }
            }
            System.out.printf("New balance: Rs. %.2f%n", acc.getBalance());
        } else if (req.getType().equals("PENDING_DEPOSIT")) {
            acc.deposit(req.getAmount());
            acc.getTransactionHistory().push(new Transaction("DEPOSIT", req.getAmount(), true));
            System.out.printf("Deposited Rs. %.2f. New balance: Rs. %.2f%n",
                    req.getAmount(), acc.getBalance());
        } else {
            System.out.println("Unknown request type. Request discarded.");
        }
    }

    private void viewPendingRequests() {
        System.out.println("Pending requests (FIFO order):");
        pendingQueue.display();
    }

    // ---------------- Session control ----------------

    private void logout() {
        user.logout();
        System.out.println("Logged out successfully.\n");
    }

    private void exit() {
        System.out.println("Thank you for using the ATM Simulator. Goodbye!");
        running = false;
    }

    // ---------------- Input helper ----------------

    /**
     * Read a whole line and parse it as an int.
     *
     * @return the entered int, or -1 if the input was non-numeric / blank.
     *         If the input stream ends (EOF) a NoSuchElementException is thrown
     *         and caught by start(), which shuts down gracefully.
     */
    private int readInt(String prompt) {
        System.out.print(prompt);
        // scanner.nextLine() throws NoSuchElementException when the input stream
        // ends; we let that propagate so start() can shut down gracefully rather
        // than loop forever returning -1.
        String line = scanner.nextLine();
        try {
            return Integer.parseInt(line.trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a valid number.");
            return -1;
        }
    }
}
