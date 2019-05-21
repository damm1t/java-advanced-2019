package ru.ifmo.rain.sokolov.rmi;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import static junit.framework.TestCase.*;

public class BankTest {
    private static final int PORT = 6666;
    private static Bank bank;
    private static Registry registry;
    private final int id = 1;
    private final String firstName = "first";
    private final String lastName = "second";
    private final String accountName = "accountName";

    @BeforeClass
    public static void beforeClass() throws RemoteException {
        registry = LocateRegistry.createRegistry(PORT);
    }

    @Before
    public void before() throws RemoteException {
        bank = new RemoteBank(PORT);
        Bank stub = (Bank) UnicastRemoteObject.exportObject(bank, 0);
        registry.rebind("bank", stub);
    }


    @Test
    public void changeMoney() throws RemoteException {
        for (var i = 0; i < 10; ++i) {
            assertTrue(bank.createPerson(id + i, firstName + i, lastName + i));
            bank.getRemotePerson(id + i);
        }

        for (var i = 0; i < 10; ++i) {
            Person person = bank.getRemotePerson(id + i);

            for (var j = 0; j < 10; ++j) {
                bank.createAccount(person, "Account" + j);
            }
        }

        for (var i = 0; i < 10; ++i) {
            Person person = bank.getRemotePerson(id + i);
            for (var j = 0; j < 10; ++j) {

                Account account = bank.getAccount(person, "Account" + j);
                System.out.println("Account id: " + account.getId());
                System.out.println("Money: " + account.getAmount());
                System.out.println("Changing amount...");
                account.setAmount(account.getAmount() + 200);
                System.out.println("Money: " + account.getAmount());
                assertEquals(200, account.getAmount());
            }
        }

        for (var i = 0; i < 10; ++i) {
            Person person = bank.getRemotePerson(id + i);
            for (var j = 0; j < 10; ++j) {

                Account account = bank.getAccount(person, "Account" + j);
                System.out.println("Account id: " + account.getId());
                System.out.println("Money: " + account.getAmount());
                System.out.println("Changing amount...");
                account.setAmount(account.getAmount() - 200);
                System.out.println("Money: " + account.getAmount());
                assertEquals(0, account.getAmount());
            }
        }
    }

    @Test
    public void createOneRemotePerson() throws RemoteException {
        assertTrue(bank.createPerson(id, firstName, lastName));
        Person remotePerson = bank.getRemotePerson(id);
        assertEquals(remotePerson.getPassportId(), id);
        assertEquals(remotePerson.getFirstName(), firstName);
        assertEquals(remotePerson.getLastName(), lastName);
    }

    @Test
    public void createOneRemoteLocalPerson() throws RemoteException {
        assertTrue(bank.createPerson(id, firstName, lastName));
        Person localPerson = bank.getLocalPerson(id);
        assertEquals(localPerson.getPassportId(), id);
        assertEquals(localPerson.getFirstName(), firstName);
        assertEquals(localPerson.getLastName(), lastName);
    }

    @Test
    public void amountAccountsByPerson() throws RemoteException {
        bank.createPerson(id, firstName, lastName);
        Person remotePerson = bank.getRemotePerson(id);
        for (var i = 0; i < 10; ++i)
            for (var j = 0; j < 10; ++j)
                bank.createAccount(remotePerson, Integer.toString(j));
        Set<String> accounts = bank.getAccountsByPerson(remotePerson);
        assertEquals(accounts.size(), 10);
    }

    @Test
    public void createOnePersonTwice() throws RemoteException {
        assertTrue(bank.createPerson(id, firstName, lastName));
        assertFalse(bank.createPerson(id, firstName, lastName));
    }

    @Test
    public void createManyPerson() throws RemoteException {
        int count = 1000;
        for (int i = 0; i < count; i++) {
            assertTrue(bank.createPerson(i, firstName + i, lastName + i));

            Person remotePerson = bank.getRemotePerson(i);
            assertEquals(remotePerson.getPassportId(), i);
            assertEquals(remotePerson.getFirstName(), firstName + i);
            assertEquals(remotePerson.getLastName(), lastName + i);

            Person localPerson = bank.getLocalPerson(i);
            assertEquals(localPerson.getPassportId(), i);
            assertEquals(localPerson.getFirstName(), firstName + i);
            assertEquals(localPerson.getLastName(), lastName + i);
        }
    }

    @Test
    public void getPersonWithoutCreating() throws RemoteException {
        assertNull(bank.getLocalPerson(9999));
        assertTrue(bank.createPerson(id, firstName, lastName));
        assertNull(bank.getLocalPerson(-1));
    }

    @Test
    public void createOneAccountRemotePerson() throws RemoteException {
        bank.createPerson(id, firstName, lastName);

        Person remotePerson = bank.getRemotePerson(id);
        assertTrue(bank.createAccount(remotePerson, accountName));
        Account account = bank.getAccount(remotePerson, accountName);
        assertEquals(account.getId(), accountName);
        assertEquals(account.getAmount(), 0);
    }

    @Test
    public void createOneAccountLocalPerson() throws RemoteException {
        bank.createPerson(id, firstName, lastName);

        Person localPerson = bank.getLocalPerson(id);
        assertTrue(bank.createAccount(localPerson, accountName));
        Account account = bank.getAccount(localPerson, accountName);
        assertEquals(account.getId(), accountName);
        assertEquals(account.getAmount(), 0);
    }

    @Test
    public void createOneAccountRemotePersonTwice() throws RemoteException {
        bank.createPerson(id, firstName, lastName);

        Person remotePerson = bank.getRemotePerson(id);
        assertTrue(bank.createAccount(remotePerson, accountName));
        assertFalse(bank.createAccount(remotePerson, accountName));
    }

    @Test
    public void getAccountWithoutCreating() throws RemoteException {
        bank.createPerson(id, firstName, lastName);

        Person person = bank.getRemotePerson(id);
        Account account = bank.getAccount(person, "1234");
        assertEquals(account.getAmount(), 0);
        var d = 322;
        account.setAmount(account.getAmount() + d);
        assertEquals(account.getAmount(), d);
        account.setAmount(account.getAmount() - d);
        assertFalse(account.getAmount() == d);
        assertEquals(account.getAmount(), 0);

    }

    @Test
    public void createOneAccountLocalPersonTwice() throws RemoteException {
        bank.createPerson(id, firstName, lastName);

        Person localPerson = bank.getLocalPerson(id);
        assertTrue(bank.createAccount(localPerson, accountName));
        assertFalse(bank.createAccount(localPerson, accountName));
    }

    @Test
    public void createManyAccount() throws RemoteException {
        int peopleCount = 1000;
        int randomMax = 1000;
        var random = new Random();
        for (int i = 0; i < peopleCount; i++) {
            bank.createPerson(i, firstName, lastName);
            Person remotePerson = bank.getRemotePerson(i);
            int accountCount = Math.abs(random.nextInt(randomMax));
            for (int j = 0; j < accountCount; j++) {
                bank.createAccount(remotePerson, accountName + j);
            }
            assertEquals(bank.getAccountsByPerson(remotePerson).size(), accountCount);
        }
    }

    @Test
    public void setAccountAmountRemotePerson() throws RemoteException {
        int amount = 322;
        bank.createPerson(id, firstName, lastName);

        Person localPerson = bank.getLocalPerson(id);

        Person remotePerson = bank.getRemotePerson(id);
        bank.createAccount(remotePerson, accountName);
        Account account = bank.getAccount(remotePerson, accountName);

        assertNull(bank.getAccount(localPerson, accountName));
        localPerson = bank.getLocalPerson(id);

        account.setAmount(amount);

        account = bank.getAccount(remotePerson, accountName);
        assertEquals(account.getId(), accountName);
        assertEquals(account.getAmount(), amount);

        account = bank.getAccount(localPerson, accountName);
        assertEquals(account.getAmount(), 0);

        localPerson = bank.getLocalPerson(id);
        account = bank.getAccount(localPerson, accountName);
        assertEquals(account.getAmount(), amount);
    }

    @Test
    public void setAccountAmountLocalPerson() throws RemoteException {
        final int amount = (int) (Math.random() * 322);
        bank.createPerson(id, firstName, lastName);

        Person localPerson = bank.getLocalPerson(id);
        bank.createAccount(localPerson, accountName);
        Account account = bank.getAccount(localPerson, accountName);
        account.setAmount(amount);

        account = bank.getAccount(localPerson, accountName);
        assertEquals(account.getAmount(), amount);

        Person remotePerson = bank.getRemotePerson(id);
        account = bank.getAccount(remotePerson, accountName);
        assertEquals(account.getAmount(), 0);
    }


    @Test
    public void checkAndCreateOnePerson() throws RemoteException {
        assertFalse(bank.checkPerson(id, firstName + id, lastName + id));
        bank.createPerson(id, firstName + id, lastName + id);
        assertTrue(bank.checkPerson(id, firstName + id, lastName + id));
    }


    @Test
    public void checkAndCreateManyPerson() throws RemoteException {
        int count = 1000;
        for (int i = 0; i < count; ++i) {
            assertFalse(bank.checkPerson(i, firstName + i, lastName + i));
            bank.createPerson(i, firstName + i, lastName + i);
            assertTrue(bank.checkPerson(i, firstName + i, lastName + i));
        }
    }

    @Test
    public void checkSomeUserCreateManyAccount() throws RemoteException {
        int peopleCount = 1000;
        int accountCount = 1000;
        bank.createPerson(id, firstName, lastName);
        List<Person> people = new ArrayList<>();
        for (int i = 0; i < peopleCount; ++i) {
            people.add(bank.getRemotePerson(id));
        }
        for (int i = 0; i < accountCount; i++) {
            for (int j = 0; j < peopleCount; j++) {
                assertTrue(bank.createAccount(people.get(j), "rmi " + j + " account " + i));
            }
        }
        for (int i = 0; i < peopleCount; i++) {
            assertEquals(bank.getAccountsByPerson(people.get(i)).size(), accountCount * peopleCount);
        }
    }

    @Test
    public void checkSomeUserSetAmount() throws RemoteException {
        int peopleCount = 100;
        int accountCount = 100;
        List<Person> people = new ArrayList<>();
        for (int i = 0; i < peopleCount; ++i) {
            bank.createPerson(i, firstName, lastName);
            people.add(bank.getRemotePerson(i));
        }
        Map<String, Integer> answer = new HashMap<>();
        Random random = new Random();
        for (int i = 0; i < accountCount; i++) {
            for (int j = 0; j < peopleCount; j++) {
                final String accountId = "rmi " + j + " account " + i;
                bank.createAccount(people.get(j), accountId);
                Account account = bank.getAccount(people.get(j), accountId);
                int value = Math.abs(random.nextInt());
                account.setAmount(value);
                answer.put(accountId, value);
            }
        }
        for (int i = 0; i < accountCount; i++) {
            for (int j = 0; j < peopleCount; j++) {
                final String accountId = "rmi " + j + " account " + i;
                assertEquals(bank.getAccount(people.get(j), accountId).getAmount(), answer.get(accountId).intValue());
            }
        }
    }


}