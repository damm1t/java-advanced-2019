package ru.ifmo.rain.sokolov.rmi;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


public class Client {
    public static void main(final String... args) throws RemoteException {
        final Bank bank;
        String firstName;
        String lastName;
        String accountId;
        int passportId;
        int change;

        try {
            Registry registry = LocateRegistry.getRegistry(1488);
            bank = (Bank) registry.lookup("bank");
        } catch (NotBoundException e) {
            error(e, "Bank is not bound");
            return;
        }

        if (args.length != 5) {
            System.err.println("Expected 5 arguments: <first name> <lastName> <passportId> <accountId> <change>.\n Found " + args.length);
            return;
        }

        for (var arg : args) {
            if (arg == null) {
                System.err.println("Expected non-null arguments");
                return;
            }
        }

        try {
            firstName = args[0];
            lastName = args[1];
            passportId = Integer.parseInt(args[2]);
            accountId = args[3];
            change = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            error(e, "Error occurred while parsing args");
            return;
        }
        System.err.println(firstName);
        System.err.println(change);
        Person person = bank.getRemotePerson(passportId);
        if (person == null) {
            System.out.println("Creating new rmi by id " + passportId);
            bank.createPerson(passportId, firstName, lastName);
            person = bank.getRemotePerson(passportId);
        }

        if (!bank.checkPerson(passportId, firstName, lastName)) {
            System.err.println("Incorrect rmi data");
            return;
        }

        Account account = bank.getAccount(person, accountId);
        System.out.println("Account id: " + account.getId());
        System.out.println("Money: " + account.getAmount());
        System.out.println("Changing amount...");
        account.setAmount(account.getAmount() + change);
        System.out.println("Money: " + account.getAmount());
    }

    private static void error(Exception e, String message) {
        System.err.println(message);
        System.err.println("Exception message: " + e.getMessage());
    }
}