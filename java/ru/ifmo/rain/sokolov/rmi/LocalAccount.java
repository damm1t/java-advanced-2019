package ru.ifmo.rain.sokolov.rmi;


import java.io.Serializable;

public class LocalAccount implements Account, Serializable {
    private final String id;
    private int amount;

    public LocalAccount(final String id) {
        this(id, 0);
    }

    public LocalAccount(final String id, final int amount) {
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