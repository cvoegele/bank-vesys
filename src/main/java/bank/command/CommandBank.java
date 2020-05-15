package bank.command;

import bank.InactiveException;
import bank.OverdrawException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CommandBank implements bank.Bank {
    public ICommandSend commandSend;

    public CommandBank(ICommandSend commandSend) {
        this.commandSend = commandSend;
    }

    @Override
    public String createAccount(String s) {
        Command result = commandSend.send(new Command(CommandType.CREATEACCOUNT, new Object[]{s}));
        return (String) result.parameters[0];
    }

    @Override
    public boolean closeAccount(String s) {
        //TRUE or FALSE
        Command result = commandSend.send(new Command(CommandType.CLOSEACCOUNT, new Object[]{s}));
        return (boolean) result.parameters[0];
    }

    @Override
    public Set<String> getAccountNumbers() {

        Command result = commandSend.send(new Command(CommandType.GETACCOUNTS, new Object[]{}));
        Set<String> resultSet = new HashSet<>();

        if (result.parameters.length != 0) {
            String[] numbers = Arrays.copyOf(result.parameters, result.parameters.length, String[].class);
            Collections.addAll(resultSet, numbers);
        }

        return resultSet;
    }

    @Override
    public Account getAccount(String s) {
        Command result = commandSend.send(new Command(CommandType.GETNUMBER, new Object[]{s}));
        if (result.parameters[0] != null) {
            return new Account(s, commandSend);
        } else {
            return null;
        }
    }

    @Override
    public void transfer(bank.Account from, bank.Account to, double v) throws IOException, IllegalArgumentException, OverdrawException, InactiveException {

        if (v < 0) throw new IllegalArgumentException();

        if (!from.isActive() || !to.isActive()) throw new InactiveException();
        double fromAccountBalance = from.getBalance();
        if (fromAccountBalance < v) throw new OverdrawException();

        double toAccountBalance = to.getBalance();

        Command result = commandSend.send(new Command(CommandType.TRANSFER, new Object[]{from.getNumber(), to.getNumber(), v}));

        //server threw exception
        if (result.parameters[0] instanceof Exception) {
            Object e = result.parameters[0];
            if (e instanceof InactiveException) {
                throw new InactiveException();
            } else if (e instanceof OverdrawException) {
                throw new OverdrawException();
            } else if (e instanceof IOException) {
                throw new IOException();
            }
        }
    }

    private static class Account implements bank.Account {

        String number;
        private final ICommandSend commandSend;


        public Account(String number, ICommandSend commandSend) {
            this.number = number;
            this.commandSend = commandSend;
        }

        @Override
        public String getNumber() {
            return number;
        }

        @Override
        public String getOwner() {
            Command result = commandSend.send(new Command(CommandType.GETOWNER, new Object[]{number}));
            return (String) result.parameters[0];
        }

        @Override
        public boolean isActive() {
            Command result = commandSend.send(new Command(CommandType.ACTIVE, new Object[]{number}));
            return (boolean) result.parameters[0];
        }

        @Override
        public void deposit(double v) throws IllegalArgumentException, InactiveException {
            if (v < 0) throw new IllegalArgumentException();
            if (!isActive()) throw new InactiveException();

            commandSend.send(new Command(CommandType.DEPOSIT, new Object[]{number, v}));

        }

        @Override
        public void withdraw(double v) throws IllegalArgumentException, OverdrawException, InactiveException {
            if (v < 0) throw new IllegalArgumentException();
            if (!isActive()) throw new InactiveException();
            if (v > getBalance()) throw new OverdrawException();

            commandSend.send(new Command(CommandType.WITHDRAW, new Object[]{number, v}));
        }

        @Override
        public double getBalance() {
            Command result = commandSend.send(new Command(CommandType.GETBALANCE, new Object[]{number}));
            return (double) result.parameters[0];
        }
    }
}
