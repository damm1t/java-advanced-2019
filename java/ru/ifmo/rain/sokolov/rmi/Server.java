package ru.ifmo.rain.sokolov.rmi;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Server {
    private final static int PORT = 8889;

    public static void main(final String... args) {
        Bank bank;
        try {
            bank = new RemoteBank(PORT);
            Bank stub = (Bank) UnicastRemoteObject.exportObject(bank, 0);

            Registry registry = LocateRegistry.createRegistry(1488);
            registry.rebind("bank", stub);
        } catch (final RemoteException e) {
            System.out.println("Cannot export object: " + e.getMessage());
            return;
        }
        System.out.println("Server started");
    }
}