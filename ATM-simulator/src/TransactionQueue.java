/**
 * TransactionQueue
 * ----------------
 * A custom array-based circular FIFO queue used to hold pending ATM requests.
 *
 * DSA concept: Queue (First In First Out). Requests are processed in the exact
 * order they were added.
 *
 * Implemented as a circular array queue with growable capacity so it never
 * overflows. No java.util collections are used.
 *
 * Operations: enqueue, dequeue, peek, isEmpty, size, display.
 */
public class TransactionQueue {

    private static final int INITIAL_CAPACITY = 10;

    private Transaction[] queue;
    private int front;     // index of the first (oldest) element
    private int rear;      // index of the last (newest) element
    private int size;      // number of elements currently stored
    private int capacity;  // current length of the backing array

    public TransactionQueue() {
        capacity = INITIAL_CAPACITY;
        queue = new Transaction[capacity];
        front = 0;
        rear = -1;
        size = 0;
    }

    /** Add a request to the rear of the queue. */
    public void enqueue(Transaction request) {
        if (request == null) {
            return;
        }
        if (size == capacity) {
            grow();
        }
        rear = (rear + 1) % capacity;   // circular wrap-around
        queue[rear] = request;
        size++;
    }

    /** Double the capacity, unwinding the circular layout into a linear one. */
    private void grow() {
        int newCapacity = capacity * 2;
        Transaction[] newQueue = new Transaction[newCapacity];
        for (int i = 0; i < size; i++) {
            newQueue[i] = queue[(front + i) % capacity];
        }
        queue = newQueue;
        front = 0;
        rear = size - 1;
        capacity = newCapacity;
    }

    /** Remove and return the front (oldest) request. Returns null if empty. */
    public Transaction dequeue() {
        if (isEmpty()) {
            return null;
        }
        Transaction t = queue[front];
        queue[front] = null;
        front = (front + 1) % capacity;   // circular wrap-around
        size--;
        return t;
    }

    /** Return the front request without removing it. Returns null if empty. */
    public Transaction peek() {
        if (isEmpty()) {
            return null;
        }
        return queue[front];
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    /** Display all pending requests from front to rear (FIFO order). */
    public void display() {
        if (isEmpty()) {
            System.out.println("  No pending requests.");
            return;
        }
        for (int i = 0; i < size; i++) {
            System.out.println("  " + (i + 1) + ". " + queue[(front + i) % capacity]);
        }
    }
}
