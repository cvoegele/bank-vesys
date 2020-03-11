/*
 * Copyright (c) 2020 Fachhochschule Nordwestschweiz (FHNW)
 * All Rights Reserved.
 */

package bank.local;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import bank.Bank;
import bank.BankDriver;
import bank.InactiveException;
import bank.OverdrawException;

public class Driver implements BankDriver {
    private Bank bank = null;

    @Override
    public void connect(String[] args) {
        bank = new Bank();
        System.out.println("connected...");
    }

    @Override
    public void disconnect() {
        bank = null;
        System.out.println("disconnected...");
    }

    @Override
    public bank.Bank getBank() {
        return bank;
    }

    public static class Bank implements bank.Bank {
    	// XXX die Implementierung ist natürlich nicht thread-safe (aber das war auch nicht verlangt)
        private final Map<String, Account> accounts = new HashMap<>();
        private final String bankIdentifier = "OFS-";

        @Override
        public Set<String> getAccountNumbers() {
            return accounts.values().stream().filter(Account::isActive).map(Account::getNumber).collect(Collectors.toSet());
        }

        @Override
        public String createAccount(String owner) {
            Account account = new Account(owner, bankIdentifier + accounts.size());
            accounts.put(account.getNumber(), account);
            return account.getNumber();
        }

        @Override
        public boolean closeAccount(String number) {
            Account account = accounts.get(number);
            if (account != null && account.isActive() && account.balance == 0) {
                accounts.get(number).active = false;
                return true;
            }
            return false;
        }

        @Override
        public bank.Account getAccount(String number) {
            return accounts.get(number);
        }

        @Override
        public void transfer(bank.Account from, bank.Account to, double amount)
                throws IOException, InactiveException, OverdrawException {
            if (from.isActive() && to.isActive()) {
                from.withdraw(amount);
                to.deposit(amount);
            } else {
                throw new InactiveException();
            }

        }

    }

    private static class Account implements bank.Account {
        private String number; // XXX ich würde number und owner final deklarieren.
        private String owner;
        private double balance;
        private boolean active = true;

        private Account(String owner, String number) {
            this.owner = owner;
            this.number = number;
            balance = 0.0;
        }

        @Override
        public double getBalance() {
            return balance;
        }

        @Override
        public String getOwner() {
            return owner;
        }

        @Override
        public String getNumber() {
            return number;
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public void deposit(double amount) throws InactiveException {
            if (!active) {
                throw new InactiveException();
            }
            if (amount < 0) {
                throw new IllegalArgumentException("negative amounts are not allowed");
            }
            balance += amount;
        }

        @Override
        public void withdraw(double amount) throws InactiveException, OverdrawException {
            if (!active) {
                throw new InactiveException("this account is not active");
            }
            if (amount < 0) {
                throw new IllegalArgumentException("negative amounts are not allowed");
            }
            if (balance - amount < 0) {
                throw new OverdrawException("you cannot overdraw the account");
            } else {
                balance -= amount;
            }

        }

    }
}
