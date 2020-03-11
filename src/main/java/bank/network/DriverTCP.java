package bank.network;

import bank.*;

import java.io.*;
import java.lang.reflect.Constructor;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class DriverTCP implements BankDriver {

    private Socket s;

    private DataOutputStream out;
    private DataInputStream in;
    private Bank bank;

    @Override
    public void connect(String[] args) throws IOException {
        String host = args[0];
        int port = Integer.parseInt(args[1]);

        try {
            s = new Socket(host, port, null, 0);
            out = new DataOutputStream(s.getOutputStream());
            in = new DataInputStream(s.getInputStream());
            bank = new Bank(out, in);
        } catch (Exception e) {
            System.out.println("disconnected.");
        }
    }

    @Override
    public void disconnect() throws IOException {
        if (s != null) {
            s.close();
        }
        bank = null;
        System.out.println("disconnected");
    }

    @Override
    public Bank getBank() {
        return bank;
    }

    public static class Bank implements bank.Bank {

        private final DataInputStream in;
        private final DataOutputStream out;

        public Bank(DataOutputStream out, DataInputStream in) {
            this.out = out;
            this.in = in;
        }

        @Override
        public String createAccount(String owner) throws IOException {
            out.writeUTF("createAccount " + owner);
            String[] response = processResponse(receiveResponse());
            if ("true".equals(response[0])) {
                return response[1];
            }
            throw new IOException("did not receive answer");
        }

        @Override
        public boolean closeAccount(String number) throws IOException {
            out.writeUTF("closeAccount " + number);
            String[] response = processResponse(receiveResponse());
            return "true".equals(response[0]);
        }

        @Override
        public Set<String> getAccountNumbers() throws IOException {
            out.writeUTF("accounts");
            String response = receiveResponse();
            System.out.println(response);

            String[] parts = processResponse(response);

            return new HashSet<>(Arrays.asList(parts).subList(1, Integer.parseInt(parts[0])));
        }

        @Override
        public Account getAccount(String number) throws IOException {
            out.writeUTF("isAccount " + number);
            String response = receiveResponse();
            if ("true".equals(response))
                return new Account(number, in, out);
            return null;
        }

        @Override
        public void transfer(bank.Account a, bank.Account b, double amount) throws IOException, IllegalArgumentException, OverdrawException, InactiveException {

            if (amount < 0) throw new IllegalArgumentException("amount is not allowed to be negative");

            out.writeUTF("transfer " + a.getNumber() + " " + b.getNumber() + " " + amount);
            String response = receiveResponse();
            if (!"true".equals(response)) {
               if ("class bank.OverdrawException".equals(response)) throw new OverdrawException("insufficient funds");
               if ("class bank.InactiveException".equals(response)) throw new InactiveException("the account is inactive");
            }

        }

        public static String[] processResponse(String response) {
            response = response.strip();
            return response.split(Pattern.quote(" "));
        }

        public String receiveResponse() throws IOException {
            return in.readUTF();
        }

    }

    public static class Account implements bank.Account {

        private final String number;

        private final DataInputStream in;
        private final DataOutputStream out;

        public Account(String number, DataInputStream in, DataOutputStream out) {
            this.number = number;
            this.in = in;
            this.out = out;
        }

        @Override
        public String getNumber() {
            return number;
        }

        @Override
        public String getOwner() throws IOException {
            out.writeUTF("getOwner " + number);
            String response = receiveResponse();

            if ("class java.io.IOException".equals(response)) {
                throw new IOException("received error");
            }

            return response;
        }

        @Override
        public boolean isActive() throws IOException {
            out.writeUTF("isActive " + number);
            String[] response = Bank.processResponse(receiveResponse());

            if ("error".equals(response[0])) {
                throw new IOException("received error");
            }

            return "true".equals(response[0]);
        }

        @Override
        public void deposit(double amount) throws IOException, IllegalArgumentException, InactiveException {

            if (amount < 0) throw new IllegalArgumentException("amount is not allowed to be negative");

            out.writeUTF("deposit " + number + " " + amount);
            String response = receiveResponse();

            if (!"true".equals(response)) {
                if ("class bank.InactiveException".equals(response)) throw new InactiveException("the account is inactive");
            }
        }

        @Override
        public void withdraw(double amount) throws IOException, IllegalArgumentException, OverdrawException, InactiveException {

            if (amount < 0) throw new IllegalArgumentException("amount is not allowed to be negative");

            out.writeUTF("withdraw " + number + " " + amount);
            String response = receiveResponse();

            if (!"true".equals(response)) {
                if ("class bank.OverdrawException".equals(response)) throw new OverdrawException("insufficient funds");
                if ("class bank.InactiveException".equals(response)) throw new InactiveException("the account is inactive");
            }
        }

        @Override
        public double getBalance() throws IOException {
            out.writeUTF("getBalance " + number);
            String[] response = Bank.processResponse(receiveResponse());

            if ("error".equals(response[0])) {
                throw new IOException("received error");
            }
            return Double.parseDouble(response[0]);
        }

        public String receiveResponse() throws IOException {
            return in.readUTF();
        }
    }

}
