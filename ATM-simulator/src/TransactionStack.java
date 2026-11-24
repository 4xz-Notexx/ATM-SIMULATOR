/**
 * TransactionStack
 * ----------------
 * A custom array-based LIFO stack used to store a customer's transaction history.
 *
 * DSA concept: Stack (Last In First Out). The most recent transaction is always
 * on top, so the transaction history naturally displays newest-first.
 *
 * Implemented with a growable array. No java.util.Stack / java.util.ArrayList
 * is used, to satisfy the "custom array-based implementation" requirement.
 *
 * Operations: push, pop, peek, isEmpty, size, display.
 */
public class TransactionStack {

    private static final int INITIAL_CAPACITY = 10;

    private Transaction[] stack;
    private int top;   // index of the current top element; -1 when the stack is empty

    public TransactionStack() {
        stack = new Transaction[INITIAL_CAPACITY];
        top = -1;
    }

    /** Push a transaction onto the top of the stack. */
    public void push(Transaction transaction) {
        if (transaction == null) {
            return;
        }
        if (top == stack.length - 1) {
            grow();
        }
        stack[++top] = transaction;
    }

    /** Double the internal array capacity when the stack is full. */
    private void grow() {
        Transaction[] newStack = new Transaction[stack.length * 2];
        for (int i = 0; i < stack.length; i++) {
            newStack[i] = stack[i];
        }
        stack = newStack;
    }

    /** Remove and return the top transaction. Returns null if the stack is empty. */
    public Transaction pop() {
        if (isEmpty()) {
            return null;
        }
        Transaction t = stack[top];
        stack[top] = null;   // help the garbage collector
        top--;
        return t;
    }

    /** Return the top transaction without removing it. Returns null if empty. */
    public Transaction peek() {
        if (isEmpty()) {
            return null;
        }
        return stack[top];
    }

    public boolean isEmpty() {
        return top == -1;
    }

    public int size() {
        return top + 1;
    }

    /**
     * Display every transaction from newest to oldest (LIFO order).
     * Traverses the backing array from the top index down to 0.
     */
    public void display() {
        if (isEmpty()) {
            System.out.println("  No transactions found.");
            return;
        }
        for (int i = top; i >= 0; i--) {
            System.out.println("  " + stack[i]);
        }
    }
}
