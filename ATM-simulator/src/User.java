import java.util.Scanner;

/**
 * User
 * ----
 * Handles user authentication: locating an account by account number (Linear
 * Search over the accounts array) and validating the PIN with a 3-attempt limit.
 *
 * DSA concept: Linear Search to find an account within the accounts array.
 */
public class User {

    private final Account[] accounts;   // shared reference to the bank's accounts
    private Account currentAccount;     // the authenticated account, null when logged out

    public User(Account[] accounts) {
        this.accounts = accounts;
        this.currentAccount = null;
    }

    public Account getCurrentAccount() {
        return currentAccount;
    }

    public boolean isAuthenticated() {
        return currentAccount != null;
    }

    public void logout() {
        currentAccount = null;
    }

    /**
     * Linear Search: scan the accounts array until an account with the given
     * number is found.
     *
     * @return the matching Account, or null if none exists.
     */
    public Account findAccount(int accountNumber) {
        for (Account account : accounts) {
            if (account.getAccountNumber() == accountNumber) {
                return account;
            }
        }
        return null;
    }

    /**
     * Perform a full login: ask for the account number (Linear Search) then the
     * PIN (max 3 attempts). On success the currentAccount is set.
     *
     * @return true if login succeeded, false otherwise.
     */
    public boolean login(Scanner scanner) {
        System.out.println("\n---------- LOGIN ----------");
        System.out.print("Enter Account Number: ");
        int accountNumber;
        try {
            accountNumber = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Account number must be numeric.");
            return false;
        }

        // Linear Search for the account.
        Account account = findAccount(accountNumber);
        if (account == null) {
            System.out.println("Account not found. Please check your account number.");
            return false;
        }

        // PIN entry with a maximum of 3 attempts.
        int attempts = 0;
        while (attempts < 3) {
            System.out.print("Enter PIN: ");
            String pinInput = scanner.nextLine().trim();
            int pin;
            try {
                pin = Integer.parseInt(pinInput);
            } catch (NumberFormatException e) {
                // Non-numeric input does not consume a PIN attempt.
                System.out.println("Invalid input. PIN must be numeric. Please try again.");
                continue;
            }
            attempts++;
            if (account.validatePin(pin)) {
                currentAccount = account;
                System.out.println("\nWelcome, " + account.getHolderName() + "!\n");
                return true;
            }
            int remaining = 3 - attempts;
            if (remaining > 0) {
                System.out.println("Incorrect PIN. " + remaining + " attempt(s) remaining.");
            } else {
                System.out.println("Incorrect PIN. Maximum attempts reached.");
            }
        }
        System.out.println("Login failed. Too many incorrect attempts. Please try again later.");
        return false;
    }
}
