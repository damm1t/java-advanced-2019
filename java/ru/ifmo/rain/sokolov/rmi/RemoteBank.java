package ru.ifmo.rain.sokolov.rmi;


import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class RemoteBank implements Bank, Remote {
    private final int port;
    private final ConcurrentMap<String, Account> accounts;
    private final ConcurrentMap<Integer, Person> persons;
    private final ConcurrentMap<Integer, Set<String>> accountsByPassportId;

    public RemoteBank(final int port) {
        this.port = port;
        accounts = new ConcurrentHashMap<>();
        persons = new ConcurrentHashMap<>();
        accountsByPassportId = new ConcurrentHashMap<>();
    }

    @Override
    public boolean createAccount(Person person, String id) throws RemoteException {
        if (person == null || id == null)
            return false;

        String accountId = person.getPassportId() + ":" + id;
        Account account = accounts.get(accountId);
        if (account != null)
            return false;

        System.out.println("Creating account " + accountId);
        account = new RemoteAccount(id);
        accounts.put(accountId, account);
        UnicastRemoteObject.exportObject(account, port);
        accountsByPassportId.get(person.getPassportId()).add(id);

        if (person instanceof LocalPerson) {
            ((LocalPerson) person).addAccount(id, new LocalAccount(id));
        }

        return true;
    }

    @Override
    public Account getAccount(Person person, String id) throws RemoteException {
        if (person == null || id == null)
            return null;

        String accountId = createAccountId(person, id);
        Account account = accounts.get(accountId);

        if (account == null) {
            createAccount(person, id.split(":")[0]);
            account = accounts.get(accountId);
        }

        System.out.println("Getting account " + accountId);

        if (person instanceof LocalPerson)
            return ((LocalPerson) person).getAccount(id);

        return account;
    }

    private String createAccountId(Person person, String id) throws RemoteException {
        return person.getPassportId() + ":" + id;
    }

    @Override
    public boolean createPerson(int passportId, String firstName, String lastName) throws RemoteException {
        if (checkPersonParameters(passportId, firstName, lastName))
            return false;

        Person result = persons.get(passportId);
        if (result != null)
            return false;

        System.out.println("Creating rmi " + firstName + " " + lastName + " with ID " + passportId + ".");

        result = new RemotePerson(passportId, firstName, lastName);
        persons.put(passportId, result);
        accountsByPassportId.put(passportId, new ConcurrentSkipListSet<>());
        UnicastRemoteObject.exportObject(result, port);
        return true;
    }

    @Override
    public boolean checkPerson(int passportId, String firstName, String lastName) throws RemoteException {
        if (checkPersonParameters(passportId, firstName, lastName))
            return false;

        System.out.println("Checking rmi " + firstName + " " + lastName + " with ID " + passportId + ".");
        Person person = persons.get(passportId);
        return person != null && person.getFirstName().equals(firstName) && person.getLastName().equals(lastName);
    }

    private boolean checkPersonParameters(int passportId, String firstName, String lastName) {
        return passportId < 0 || firstName == null || lastName == null;
    }

    @Override
    public Person getLocalPerson(int passportId) throws RemoteException {
        if (passportId < 0)
            return null;

        Person person = persons.get(passportId);
        if (person == null)
            return null;

        System.out.println("Getting local rmi by passport " + passportId + ".");

        Set<String> accountNames = getAccountsByPerson(person);
        Map<String, LocalAccount> accounts = new ConcurrentHashMap<>();
        accountNames.forEach(x -> {
            try {
                Account account = getAccount(person, x);
                accounts.put(x, new LocalAccount(account.getId(), account.getAmount()));
            } catch (RemoteException e) {
                System.out.println("Error occurred while creating the local accounts");
            }
        });
        return new LocalPerson(person.getPassportId(), person.getFirstName(), person.getLastName(), accounts);
    }

    @Override
    public Person getRemotePerson(int passportId) {
        if (passportId < 0)
            return null;
        System.out.println("Getting remote rmi by passport " + passportId);
        return persons.get(passportId);
    }

    @Override
    public Set<String> getAccountsByPerson(Person person) throws RemoteException {
        if (person == null)
            return null;
        System.out.println("Getting accounts for rmi by passport " + person.getPassportId());
        if (person instanceof LocalPerson)
            return ((LocalPerson) person).getAccounts();
        return accountsByPassportId.get(person.getPassportId());
    }
}
