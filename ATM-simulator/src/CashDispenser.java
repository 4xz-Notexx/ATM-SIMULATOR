/**
 * CashDispenser
 * -------------
 * Dispenses cash using a Greedy algorithm: always try the largest denomination
 * first, then move to smaller ones.
 *
 * DSA concept: Greedy Algorithm. For the Indian denomination set
 * {500, 200, 100, 50, 20, 10} greedy selection is optimal (a canonical system),
 * so the fewest notes are used and any amount that is a multiple of 10 can be
 * paid, provided enough notes exist.
 *
 * The dispenser also tracks how many notes of each denomination remain, so it
 * can refuse a withdrawal when the ATM physically cannot make the amount.
 */
public class CashDispenser {

    // Denominations sorted largest-first; the greedy loop relies on this order.
    private static final int[] DENOMINATIONS = {500, 200, 100, 50, 20, 10};

    // Available note counts, parallel to DENOMINATIONS.
    private final int[] noteCounts;

    public CashDispenser() {
        // Initial ATM stock of notes (dummy educational values).
        noteCounts = new int[] {100, 100, 200, 200, 200, 300};
    }

    /**
     * Compute how many of each note would be dispensed for the given amount
     * WITHOUT changing the dispenser state.
     *
     * @return int[] of counts parallel to DENOMINATIONS, or null if the amount
     *         cannot be dispensed exactly with the notes available.
     */
    public int[] getDispensePlan(int amount) {
        if (amount <= 0 || amount % 10 != 0) {
            return null;
        }
        int[] plan = new int[DENOMINATIONS.length];
        int remaining = amount;
        for (int i = 0; i < DENOMINATIONS.length; i++) {
            int needed = remaining / DENOMINATIONS[i];       // how many of this denom we would like
            int usable = Math.min(needed, noteCounts[i]);    // limited by what we actually have
            plan[i] = usable;
            remaining -= usable * DENOMINATIONS[i];
            if (remaining == 0) {
                break;
            }
        }
        if (remaining != 0) {
            return null;   // cannot make exact change with available notes
        }
        return plan;
    }

    /** Check whether the amount can be dispensed (does not change state). */
    public boolean canDispense(int amount) {
        return getDispensePlan(amount) != null;
    }

    /**
     * Dispense the amount: commit the note counts and return the note plan.
     *
     * @return int[] note plan, or null if dispensing is impossible.
     */
    public int[] dispense(int amount) {
        int[] plan = getDispensePlan(amount);
        if (plan == null) {
            return null;
        }
        for (int i = 0; i < DENOMINATIONS.length; i++) {
            noteCounts[i] -= plan[i];
        }
        return plan;
    }

    /**
     * Accept previously dispensed notes back into the ATM (used when a cash
     * withdrawal is reversed). The notes array must be parallel to DENOMINATIONS.
     */
    public void returnNotes(int[] notes) {
        if (notes == null || notes.length != DENOMINATIONS.length) {
            return;
        }
        for (int i = 0; i < DENOMINATIONS.length; i++) {
            if (notes[i] > 0) {
                noteCounts[i] += notes[i];
            }
        }
    }

    /** Total cash currently held by the ATM. */
    public int totalCashAvailable() {
        int total = 0;
        for (int i = 0; i < DENOMINATIONS.length; i++) {
            total += DENOMINATIONS[i] * noteCounts[i];
        }
        return total;
    }

    public int[] getDenominations() {
        return DENOMINATIONS;
    }
}
