# ATM SIMULATOR – RUN GUIDE

## 1. Project Overview

**Project Name:** ATM Simulator  
**Technology:** Core Java, DSA, HTML, CSS, JavaScript  
**Java Version:** Java 17 or later  
**Backend Server:** Java HTTP Server  
**Default Port:** 8080

This project simulates basic ATM operations using Java and Data Structures and Algorithms.

## 2. Project Structure

```text
ATM-SIMULATOR/
│
├── bin/
│   └── Compiled Java Classes
│
├── src/
│   ├── Main.java
│   ├── ServerMain.java
│   ├── AtmServer.java
│   ├── ATM.java
│   ├── Account.java
│   ├── User.java
│   ├── Transaction.java
│   ├── TransactionStack.java
│   ├── TransactionQueue.java
│   ├── CashDispenser.java
│   └── Json.java
│
├── frontend/
│   ├── index.html
│   ├── css/
│   └── js/
│
├── README.md
├── RUN.md
└── .gitignore
```

## 3. Prerequisites

Before running the project, ensure that you have installed:

- Java JDK 17 or later.
- Visual Studio Code.
- Java Extension Pack for VS Code.
- Google Chrome or any modern web browser.

Verify Java installation:

```bash
java -version
```

Verify Java compiler:

```bash
javac -version
```

## 4. How to Run the Project

### Step 1: Open Project

Open the ATM-SIMULATOR folder in Visual Studio Code.

### Step 2: Open Terminal

Press:

```text
Ctrl + ~
```

Make sure the terminal is located in the project root directory.

### Step 3: Compile Java Files

Execute:

```bash
javac -d bin src/*.java
```

This command compiles all Java source files and stores the generated class files inside the bin directory.

### Step 4: Start the ATM Web Server

Execute:

```bash
java -cp bin ServerMain
```

The web server will start on port 8080 by default.

### Step 5: Open ATM Simulator

Open your browser and visit:

```text
http://localhost:8080
```

The ATM Simulator touchscreen interface should appear.

**IMPORTANT:** Keep the terminal running while using the application. Closing the terminal will stop the server.

## 5. Alternative: Run Console Application

To run the original console-based ATM application, execute:

```bash
java -cp bin Main
```

Note: The console application and web application use different entry points.

For the graphical ATM touchscreen interface, always use:

```bash
java -cp bin ServerMain
```

## 6. Demo Login Credentials

Use the sample account credentials configured in the Java source code.

Example accounts specified for the educational project:

| Account Number | PIN |
|---|---|
| 1001 | 1234 |
| 1002 | 2345 |
| 1003 | 3456 |

These credentials work only if the corresponding sample accounts have been initialized in the backend.

## 7. DSA Concepts Implemented

The project demonstrates the following concepts:

1. **Arrays:** Store account information and transaction records.
2. **Stack:** Manage transactions using LIFO.
3. **Queue:** Process pending requests using FIFO.
4. **Linear Search:** Find accounts using account numbers.
5. **Greedy Algorithm:** Select currency denominations during cash withdrawal.

## 8. Common Errors and Solutions

### Error: javac is not recognized

Solution:
- Install Java JDK.
- Configure the Java PATH environment variable.
- Restart VS Code.

### Error: Could not find or load main class ServerMain

Solution:

Recompile the project:

```bash
javac -d bin src/*.java
```

Then execute:

```bash
java -cp bin ServerMain
```

### Error: Address already in use

Port 8080 may already be occupied.

Try another port:

```bash
java -cp bin ServerMain 8081
```

Then open:

```text
http://localhost:8081
```

### Error: Webpage not opening

Check:
- Whether the Java server is running.
- Whether port 8080 is correct.
- Whether AtmServer.java serves the frontend index.html.
- Whether the frontend API requests use the correct server address.

## 9. Important Notes

- This is an educational ATM simulation.
- It does not connect to any real banking network.
- Account data is stored in memory in this version.
- Restarting the server may reset account balances and transactions.
- Never use real banking credentials.
- Keep the backend terminal running during operation.

## 10. Quick Start Commands

For quick execution, use these two commands from the project root:

**Compile:**

```bash
javac -d bin src/*.java
```

**Run:**

```bash
java -cp bin ServerMain
```

Open:

http://localhost:8080

---

**Developed as a B.Tech Computer Science & Engineering DSA Java Project.**