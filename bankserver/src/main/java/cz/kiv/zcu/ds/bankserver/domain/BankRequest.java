package cz.kiv.zcu.ds.bankserver.domain;

public class BankRequest {

    private int sender;

    private int amount;

    private String operation;

    public BankRequest() {
    }

    public int getSender() {
        return sender;
    }

    public void setSender(int sender) {
        this.sender = sender;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

}
