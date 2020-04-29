package bank.graphql.models;

public class Account {
    private String number;
    private String owner;
    private boolean active;
    private float balance;

    public Account(String id, String owner, Boolean active, Float balance) {
        this.number = id;
        this.owner = owner;
        this.active = active;
        this.balance = balance;
    }

    public Account(bank.Account account) {
        try {
            this.number = account.getNumber();
            this.owner = account.getOwner();
            this.active = account.isActive();
            this.balance = (float) account.getBalance();
        } catch (Exception ignored){ //Does not happen anyway
        }
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public boolean getActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public float getBalance() {
        return balance;
    }

    public void setBalance(float balance) {
        this.balance = balance;
    }
}
