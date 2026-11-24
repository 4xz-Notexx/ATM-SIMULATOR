/**
 * Account
 * -------
 * Holds a single bank account's data and manages its balance plus its
 * transaction history.
 *
 * DSA note: each account owns its own TransactionStack so that every customer's
 * history is independent and stored newest-first (LIFO).
 */
public class Account {

    private final int accountNumber;
    private int pin;
    private final String holderName;
    private double balance;
    private final TransactionStack transactionHistory;

    public Account(int accountNumber, int pin, String holderName, double balance) {
        this.accountNumber = accountNumber;
        this.pin = pin;
        this.holderName = holderName;
        this.balance = balance;
        this.transactionHistory = new TransactionStack();
    }

    public int getAccountNumber() {
        return accountNumber;
    }

    public String getHolderName() {
        return holderName;
    }

    public double getBalance() {
        return balance;
    }

    public TransactionStack getTransactionHistory() {
        return transactionHistory;
    }

    /** Check whether the entered PIN matches the stored PIN. */
    public boolean validatePin(int enteredPin) {
        return this.pin == enteredPin;
    }

    /** Allow the PIN to be changed (kept for completeness / future use). */
    public void setPin(int newPin) {
        this.pin = newPin;
    }

    /** Add money to the account. */
    public void deposit(double amount) {
        balance += amount;
    }

    /**
     * Remove money from the account.
     *
     * @return true on success, false if the balance is insufficient.
     */
    public boolean withdraw(double amount) {
        if (amount <= 0 || amount > balance) {
            return false;
        }
        balance -= amount;
        return true;
    }
}
