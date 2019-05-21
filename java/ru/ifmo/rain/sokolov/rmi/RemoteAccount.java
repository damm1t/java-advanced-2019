package ru.ifmo.rain.sokolov.rmi;


public class RemoteAccount implements Account {
    private final String id;
    private int amount;

    public RemoteAccount(final String id) {
        this(id, 0);
    }

    public RemoteAccount(final String id, final int amount) {
        this.id = id;
        this.amount = amount;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public synchronized int getAmount() {
        System.out.println("Getting amount of money for account " + id);
        return amount;
    }

    @Override
    public synchronized void setAmount(int amount) {
        System.out.println("Setting amount of money for account " + id);
        this.amount = amount;
    }
}
