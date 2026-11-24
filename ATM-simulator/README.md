# ATM Simulator using Core Java & DSA

An ATM Simulator built with **Core Java** and **Object-Oriented Programming**
that demonstrates practical applications of fundamental **Data Structures &
Algorithms (DSA)** concepts.

The project ships with **two user interfaces** powered by the same Java + DSA
backend:

1. A **console interface** (`Main`) — the original interactive terminal app.
2. A **web touchscreen interface** (`ServerMain`) — a realistic ATM UI in the
   browser, served by a thin HTTP adapter that reuses the exact same backend
   classes. The DSA and business logic stay 100% in Java.

This is a college-level B.Tech CSE (AI & ML) DSA mini-project. It is an
**educational simulator**, not a real banking application. All data is held in
memory and is lost when the program exits.

---

## Table of Contents
1. [Project Introduction](#project-introduction)
2. [Objectives](#objectives)
3. [Features](#features)
4. [Technologies Used](#technologies-used)
5. [DSA Concepts Explained](#dsa-concepts-explained)
6. [Class Descriptions](#class-descriptions)
7. [Project Architecture](#project-architecture)
8. [Installation Instructions](#installation-instructions)
9. [Compilation and Execution Commands](#compilation-and-execution-commands)
10. [Sample Login Credentials](#sample-login-credentials)
11. [Sample Output (Console)](#sample-output-console)
12. [Web Frontend (Touchscreen UI)](#web-frontend-touchscreen-ui)
13. [Testing](#testing)
14. [Future Scope](#future-scope)
15. [Known Limitations](#known-limitations)

---

## Project Introduction

The ATM Simulator mimics the basic workflow of a real Automated Teller Machine:
a user authenticates with an account number and PIN, then performs banking
operations such as checking the balance, withdrawing cash, depositing cash,
viewing transaction history, processing pending requests and reversing a
transaction.

What makes this project suitable for a DSA course is that every core data
structure and algorithm it needs is **implemented from scratch** using plain
arrays — no Java Collections (`ArrayList`, `java.util.Stack`, `LinkedList`,
etc.) are used for the required DSA parts. Both the console app and the web
adapter call these same classes, so the DSA behaviour is identical in both
interfaces.

---

## Objectives

- Apply Core Java and OOP (encapsulation, constructors, getters/setters) to
  model a real-world system.
- Implement and use **Arrays**, a custom **Stack (LIFO)**, a custom **Queue
  (FIFO)**, **Linear Search** and a **Greedy Algorithm** in one cohesive
  application.
- Build a robust console application with thorough input validation and error
  handling so it never crashes on bad input.
- Expose the same backend to a browser-based touchscreen UI through a thin HTTP
  adapter — without modifying any existing backend class or DSA code.
- Keep the code beginner-friendly, well-commented and easy to explain in a
  college viva.

---

## Features

### Banking (shared by both interfaces)
- **Login system** with account-number lookup (Linear Search) and a 3-attempt
  PIN limit. The stored PIN is never displayed.
- **Balance inquiry** with INR formatting.
- **Cash withdrawal** validated against both the account balance and the ATM's
  physical note inventory, with a denomination-wise note breakdown.
- **Cash deposit** with input validation.
- **Transaction history** displayed newest-first using the custom Stack.
- **Pending transaction requests** managed with the custom Queue and processed
  in FIFO order.
- **Transaction reversal** of the most recent eligible transaction, with
  protection against reversing the same transaction twice (the ATM's notes are
  returned on reversal, keeping cash inventory consistent).
- **Greedy currency dispensing** for Indian denominations
  (₹500, ₹200, ₹100, ₹50, ₹20, ₹10).
- Comprehensive **error handling** for invalid account numbers, wrong PINs,
  insufficient balance, insufficient ATM cash, non-numeric input, negative
  amounts, empty stacks/queues and invalid menu choices.

### Web touchscreen UI
- Realistic dark-navy ATM machine frame with banking-blue/cyan accents.
- On-screen numeric keypad with **PIN masking**.
- Quick-amount buttons for withdrawals and a printable receipt screen.
- Loading indicators and success/error toast notifications.
- A dedicated **DSA Demonstration** screen showing live results from the Java
  backend (Stack, Queue, Linear Search, Greedy) — no front-end simulation.
- Responsive layout for desktop and mobile.

---

## Technologies Used

| Item               | Detail                          |
|-------------------|---------------------------------|
| Language           | Java                            |
| Java Version       | Java 17 or later (tested on JDK 25) |
| Backend interface  | Console (`java.util.Scanner`) and HTTP (`com.sun.net.httpserver`, bundled with the JDK) |
| Frontend           | HTML5, CSS3, Vanilla JavaScript, Fetch API (no frameworks, no external CDNs) |
| Libraries          | Core Java only (`java.util.Scanner`, `java.time.*`, `com.sun.net.httpserver`) |
| Database           | None — in-memory sample data    |
| IDE                | VS Code compatible               |

---

## DSA Concepts Explained

### A. Arrays
The bank's accounts are stored in a fixed-size `Account[]` array inside the
`ATM` (console) and `AtmServer` (web) classes. The array is traversed during
login to find a matching account. This is the simplest container and the
foundation for the Linear Search.

### B. Stack (LIFO) — `TransactionStack`
A custom **array-based** stack with `push`, `pop`, `peek`, `isEmpty`, `size`
and `display`. It grows its internal array (doubling capacity) when full. Each
account owns its own `TransactionStack`, so the transaction history is shown
**newest-first** because the most recent transaction is always on top. Reversal
uses `peek` to inspect the latest transaction.

### C. Queue (FIFO) — `TransactionQueue`
A custom **circular array-based** queue with `enqueue`, `dequeue`, `peek`,
`isEmpty`, `size` and `display`. It also grows when full. Pending ATM requests
(deposits/withdrawals that need "processing") are added to the rear and
processed from the front, guaranteeing First-In-First-Out order.

### D. Searching Algorithm — Linear Search
`User.findAccount(int accountNumber)` performs a Linear Search across the
accounts array, returning the matching `Account` or `null` if none is found.
This is the account-lookup step of the login process (used by both the console
app and the web adapter).

### E. Greedy Algorithm — `CashDispenser`
Currency dispensing uses a Greedy strategy: for each denomination **from
largest to smallest**, take as many notes as possible (limited by what the ATM
actually holds) and reduce the remaining amount. For the Indian denomination
set `{500, 200, 100, 50, 20, 10}` this greedy choice is optimal (a canonical
currency system), so it produces the fewest notes and can pay any multiple of
₹10 provided enough notes exist. If exact change is impossible, the withdrawal
is refused and **no money is deducted**.

---

## Class Descriptions

### Backend (Java, `src/`)

| Class | Responsibility |
|-------|----------------|
| `Main` | Console entry point. Creates the `ATM` and starts it. |
| `User` | Authentication: Linear Search for an account and 3-attempt PIN validation. Holds the currently authenticated account. |
| `Account` | Stores account number, PIN, holder name and balance. Owns a `TransactionStack` for its history. Provides `deposit`/`withdraw`. |
| `ATM` | Console controller. Owns the accounts array, `CashDispenser`, `TransactionQueue` and `User`. Runs the login → menu → operation loop. |
| `Transaction` | Data unit: transaction id, type, amount, timestamp, reversal status and the dispensed-note breakdown (for reversal). Used by both the stack and the queue. |
| `TransactionStack` | Custom array-based LIFO stack for transaction history. |
| `TransactionQueue` | Custom circular array-based FIFO queue for pending requests. |
| `CashDispenser` | Greedy denomination selection; tracks the ATM's note inventory. |

### HTTP adapter (Java, `src/`) — added for the web UI, does **not** modify the backend
| Class | Responsibility |
|-------|----------------|
| `ServerMain` | Web entry point. Starts the `AtmServer` HTTP server. |
| `AtmServer` | Serves the static frontend and exposes a small JSON API. Reuses `User.findAccount`, `Account`, `CashDispenser`, `TransactionStack` and `TransactionQueue` directly — no duplication of DSA logic. |
| `Json` | Tiny JSON string/number helpers for building API responses (no external library). |

### Frontend (`frontend/`)
| File | Responsibility |
|------|----------------|
| `index.html` | All screens in one shell: welcome, login, menu, balance, withdraw, deposit, history, receipt, services, pending, DSA. |
| `css/style.css` | ATM frame, screen, buttons, keypad, receipts, responsive + print rules. |
| `css/animations.css` | Screen transitions, spinner, toasts, keypad press. |
| `js/api.js` | Fetch API client calling the real backend endpoints. |
| `js/keypad.js` | On-screen numeric keypad with PIN masking. |
| `js/dsa-visualizer.js` | Renders live backend DSA results (Stack, Queue, Linear Search, Greedy). |
| `js/app.js` | Controller: navigation, state and wiring every button to the API. |

---

## Project Architecture

```
                            ┌─────────────────────────────┐
   Console users ──────────▶│  Main  ──▶  ATM (Scanner)   │
                            └──────────────┬──────────────┘
                                           │  reuses
   Browser users  ──────────┐              ▼
   (Fetch API)              │   ┌───────────────────────┐
        │                   └──▶│  ServerMain ─▶ AtmServer│  (HTTP adapter)
        ▼                       │   serves frontend +    │
   frontend/ (HTML/CSS/JS) ──────▶   JSON API              │
                                 └───────────┬───────────┘
                                             │  reuses (same backend)
                                             ▼
   ┌──────────────── shared Java + DSA backend ────────────────┐
   │ User (Linear Search)   Account[]   CashDispenser (Greedy)  │
   │ TransactionStack (LIFO)  TransactionQueue (FIFO)  Transaction│
   └────────────────────────────────────────────────────────────┘
```

**Key point:** the web adapter is a *thin* layer. It does **not** reimplement any
DSA or business logic — it calls `User.findAccount`, `Account.deposit/withdraw`,
`CashDispenser.getDispensePlan/dispense`, and the `TransactionStack` /
`TransactionQueue` of the existing classes. The console app (`Main`/`ATM`) is
untouched and still works exactly as before.

---

## Installation Instructions

1. Install a **JDK 17 or later** and ensure `javac` and `java` are on your
   `PATH`. Verify with:
   ```powershell
   javac -version
   java -version
   ```
2. Open the project folder `ATM-simulator` in VS Code.
3. No build tool and no external libraries are required (the HTTP server uses
   `com.sun.net.httpserver`, which ships with the JDK).

---

## Compilation and Execution Commands

From the project root folder (`ATM-simulator`).

### Compile (both interfaces at once)
```powershell
javac -d bin src/*.java
```
> If your shell does not expand `src/*.java`, use the sourcepath form:
> ```powershell
> javac -d bin -sourcepath src src/Main.java
> ```

### Run the console app
```powershell
java -cp bin Main
```

### Run the web / touchscreen app
```powershell
java -cp bin ServerMain
```
Then open **http://localhost:8080** in a browser.
(Optional port: `java -cp bin ServerMain 9090`.)

Press `Ctrl+C` in the terminal to stop the server.

---

## Sample Login Credentials

These are dummy, educational accounts only.

| Account Number | PIN  | Name          | Balance  |
|---------------|------|---------------|----------|
| 1001          | 1234 | Anas Ahmad    | ₹5,000   |
| 1002          | 2345 | Rahul Sharma  | ₹10,000  |
| 1003          | 3456 | Aman Kumar    | ₹7,500   |

---

## Sample Output (Console)

```
====================================
          ATM SIMULATOR
====================================
---------- LOGIN ----------
Enter Account Number: 1001
Enter PIN: 1234

Welcome, Anas Ahmad!

--------------- ATM MENU ---------------
Account: 1001  |  Holder: Anas Ahmad
1. Check Balance
2. Withdraw Cash
3. Deposit Cash
4. Transaction History
5. Pending Transaction Requests
6. Reverse Last Eligible Transaction
7. Logout
8. Exit
---------------------------------------
Enter your choice: 1

--- Balance Inquiry ---
Current Balance: Rs. 5000.00

Enter your choice: 2

--- Cash Withdrawal ---
Enter withdrawal amount (multiples of 10): 1330
Withdrawal successful!
Denomination breakdown:
  Rs. 500 x 2 = Rs. 1000
  Rs. 200 x 1 = Rs. 200
  Rs. 100 x 1 = Rs. 100
  Rs.  20 x 1 = Rs. 20
  Rs.  10 x 1 = Rs. 10
Total dispensed: Rs. 1330
Remaining balance: Rs. 3670.00

Enter your choice: 4

--- Transaction History (newest first) ---
  TxnID: 1001 | WITHDRAW               | Rs.  1330.00 | 25-11-2026 03:19:24

Enter your choice: 6

--- Reverse Last Eligible Transaction ---
Transaction reversed successfully!
Reversed amount: Rs. 1330.00
New balance: Rs. 5000.00

Enter your choice: 8
Thank you for using the ATM Simulator. Goodbye!
```

---

## Web Frontend (Touchscreen UI)

A realistic ATM touchscreen built with HTML5, CSS3 and vanilla JavaScript that
talks to the Java backend over HTTP using the Fetch API.

### How the integration works
The original backend was console-only (`Scanner`/`System.out`), so a browser
cannot talk to it directly. Instead of modifying the backend, a thin HTTP
adapter (`ServerMain`/`AtmServer`) was added. It:

- serves the static frontend files from `frontend/` on the same origin (no CORS
  issues), and
- exposes a small JSON API whose handlers call the **existing** backend
  classes — `User.findAccount` (Linear Search), `Account.deposit/withdraw`,
  `CashDispenser.getDispensePlan/dispense` (Greedy), and the per-account
  `TransactionStack` plus the shared `TransactionQueue`.

Because it reuses the real classes, every result shown in the browser is a real
backend result — there is no front-end-only simulation.

### Frontend structure
```
frontend/
├── index.html
├── css/
│   ├── style.css
│   └── animations.css
└── js/
    ├── api.js
    ├── keypad.js
    ├── dsa-visualizer.js
    └── app.js
```

### Screens
Welcome → Login (keypad + PIN masking) → Main menu → Balance inquiry →
Withdraw cash (quick amounts + custom) → Deposit cash → Transaction history →
Receipt (printable) → Other services → Pending requests (queue) → DSA
demonstration.

### Starting the web app
```powershell
# from the project root
javac -d bin src/*.java
java -cp bin ServerMain
```
Then open **http://localhost:8080**. Use a demo account, e.g. `1001` / `1234`.

### JSON API exposed by AtmServer
| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/login?accountNumber=&pin=` | Authenticate (Linear Search + PIN) |
| POST | `/api/logout` | End the session |
| GET  | `/api/session` | Current session status |
| GET  | `/api/balance` | Current balance |
| POST | `/api/withdraw?amount=` | Withdraw (Greedy dispensing) |
| POST | `/api/deposit?amount=` | Deposit |
| GET  | `/api/history` | Transaction history (Stack, newest first) |
| POST | `/api/reverse` | Reverse last eligible transaction |
| POST | `/api/pending/add?type=&amount=` | Enqueue a pending request |
| POST | `/api/pending/process` | Dequeue + process next (FIFO) |
| GET  | `/api/pending/list` | List pending requests |
| GET  | `/api/dsa/stack` | Live TransactionStack contents (LIFO) |
| GET  | `/api/dsa/queue` | Live TransactionQueue contents (FIFO) |
| POST | `/api/dsa/search?accountNumber=` | Linear Search demo |
| POST | `/api/dsa/greedy?amount=` | Greedy note plan (dry run, no state change) |

### DSA Demonstration screen
- **Stack** — renders the current account's `TransactionStack` top-down (newest
  first) using `/api/dsa/stack`.
- **Queue** — renders the `TransactionQueue` front-to-rear using
  `/api/dsa/queue`.
- **Linear Search** — enter an account number; `/api/dsa/search` runs the
  backend `User.findAccount` and reports the index found and elements scanned.
- **Greedy** — enter an amount; `/api/dsa/greedy` returns the denomination
  breakdown produced by `CashDispenser.getDispensePlan` (a dry run that does not
  alter the ATM's note inventory).

---

## Testing

### Console scenarios
| # | Scenario | Result |
|---|----------|--------|
| 1  | Successful login | Works — shows welcome message |
| 2  | Incorrect PIN (3 attempts) | Works — locks out after 3 wrong attempts |
| 3  | Account not found | Works — "Account not found" message |
| 4  | Balance inquiry | Works — displays INR-formatted balance |
| 5  | Successful deposit | Works — balance updated |
| 6  | Successful withdrawal | Works — notes dispensed, balance reduced |
| 7  | Insufficient balance | Works — withdrawal refused |
| 8  | Insufficient ATM cash | Works — withdrawal refused, balance unchanged |
| 9  | Greedy denomination selection | Works — e.g. ₹1330 → 2×₹500 + 1×₹200 + 1×₹100 + 1×₹20 + 1×₹10 |
| 10 | Stack push/pop | Works — history shown newest-first (LIFO) |
| 11 | Queue enqueue/dequeue | Works — requests processed FIFO |
| 12 | Transaction reversal | Works — reverses last eligible txn; blocks double-reversal |
| 13 | Invalid user inputs | Works — non-numeric, negative, invalid menu choices handled |
| 14 | Logout and exit | Works — returns to login / terminates cleanly |

### Web integration (HTTP API verified via PowerShell `Invoke-RestMethod`)
| # | Scenario | Result |
|---|----------|--------|
| 1  | `/api/session` before login | Returns `authenticated: false` |
| 2  | Login wrong PIN | Returns `success: false` with attempts left |
| 3  | Login correct | Returns name + balance |
| 4  | `/api/balance` | Returns current balance |
| 5  | `/api/withdraw?amount=1330` | Returns breakdown `500:2, 200:1, 100:1, 50:0, 20:1, 10:1`, balance updated |
| 6  | `/api/history` | Returns transaction list newest-first |
| 7  | `/api/dsa/search?accountNumber=1002` | Found at index 1, 2 elements scanned |
| 8  | `/api/dsa/greedy?amount=1770` | Returns `500:3, 200:1, 50:1, 20:1` (6 notes) |
| 9  | `/api/pending/add` + `/api/pending/list` | Enqueue + list works |
| 10 | `/api/reverse` | Reverses withdrawal, restores balance and notes |
| 11 | Static file serving | `/index.html`, `/css/*`, `/js/*` served with correct content types |

---

## Future Scope

- Persist accounts and transactions to a file or a real database (JDBC/SQLite).
- PIN change, mini-statement, fund transfer between accounts.
- Admin mode to refill the ATM's note inventory and view it.
- Multi-threading / multiple concurrent sessions in the web adapter (currently
  single-session, single-threaded).
- Encryption of stored PINs and password hashing.
- Replace Linear Search with Binary Search (after sorting accounts) for faster
  lookups, or use a HashMap for O(1) access — and compare performance.

---

## Known Limitations

- **In-memory only:** all data is lost when the program exits (no persistence).
- **Single-user session:** one customer is authenticated at a time — this
  applies to both the console app and the web adapter.
- **Web adapter state is in-process:** restarting `ServerMain` resets all
  accounts, balances and the ATM note inventory to the sample defaults.
- **PINs are plain integers** stored in memory — not secure; educational only.
- **Double precision for balance:** amounts are integer rupees so there are no
  rounding issues, but the project does not model paise (coins).
- The ATM starts with a fixed note inventory; deposits do not refill the
  dispenser, so the ATM can eventually run out of notes.
- Linear Search is O(n); fine for a few accounts, but not for a real bank.
```
