package cz.kiv.zcu.ds.bankserver.domain;

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

    public synchronized boolean debit(long amount) {
        if (this.balance - amount < 0) return false;

        this.balance -= amount;
        return true;
    }

    public int getBalance() {
        return balance;
    }

}
