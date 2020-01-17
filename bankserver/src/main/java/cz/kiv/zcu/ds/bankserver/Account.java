package cz.kiv.zcu.ds.bankserver;

public class Account {

    private volatile int balance;

    private static Account instance;

    private Account() {
        balance = 5000000;
    }

    public static Account getInstance() {
        if (instance == null) {
            instance = new Account();
        }

        return instance;
    }

    public synchronized void credit(long amount) {
        this.balance += amount;
    }

    public synchronized void debit(long amount) {
        this.balance -= amount;
    }

    public int getBalance() {
        return balance;
    }

}
