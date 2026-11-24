import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Transaction
 * -----------
 * Represents a single banking transaction (withdraw, deposit, reversal) or a
 * pending request held in the processing queue.
 *
 * This class is the fundamental data unit used by both the TransactionStack
 * (LIFO history) and the TransactionQueue (FIFO pending requests).
 */
public class Transaction {

    // Shared counter so every transaction gets a unique id across the whole app.
    private static int idCounter = 1000;

    private final int transactionId;
    private final String type;          // WITHDRAW, DEPOSIT, REVERSAL, PENDING_WITHDRAW, PENDING_DEPOSIT
    private final double amount;
    private final LocalDateTime timestamp;
    private boolean reversed;           // true once this transaction has been reversed
    private final boolean reversible;   // whether this transaction may be reversed later
    // For cash withdrawals: the note breakdown actually dispensed, parallel to
    // CashDispenser.DENOMINATIONS. Null for non-cash transactions. Stored so a
    // reversal can return the exact notes to the ATM and keep inventory consistent.
    private int[] dispensedNotes;

    public Transaction(String type, double amount, boolean reversible) {
        this.transactionId = ++idCounter;
        this.type = type;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
        this.reversed = false;
        this.reversible = reversible;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public String getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    /** Human readable date/time string, e.g. 25-11-2026 14:30:05. */
    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
    }

    public boolean isReversed() {
        return reversed;
    }

    /** A transaction is reversible only if it was created reversible AND not yet reversed. */
    public boolean canReverse() {
        return reversible && !reversed;
    }

    public void markReversed() {
        this.reversed = true;
    }

    /** Note breakdown dispensed for a cash withdrawal (null if not applicable). */
    public int[] getDispensedNotes() {
        return dispensedNotes;
    }

    public void setDispensedNotes(int[] dispensedNotes) {
        this.dispensedNotes = dispensedNotes;
    }

    @Override
    public String toString() {
        String status = reversed ? " [REVERSED]" : "";
        return String.format("TxnID: %d | %-30s | Rs. %8.2f | %s%s",
                transactionId, type, amount, getFormattedTimestamp(), status);
    }
}
