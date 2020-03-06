package bank.network;

import bank.*;

import java.io.*;
import java.net.Socket;
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
            sendCommand("createAccount " + owner);
            String[] response = processResponse(receiveResponse());
            if ("true".equals(response[0])) {
                return response[1];
            }
            throw new IOException("did not receive answer");
        }

        @Override
        public boolean closeAccount(String number) throws IOException {
            sendCommand("closeAccount " + number);
            String[] response = processResponse(receiveResponse());
            return "true".equals(response[0]);
        }

        @Override
        public Set<String> getAccountNumbers() throws IOException {
            sendCommand("accounts");
            String response = receiveResponse();
            System.out.println(response);

            String[] parts = processResponse(response);

            Set<String> accounts = new HashSet<>();

            for (int i = 1; i < Integer.parseInt(parts[0]); i++) {
                accounts.add(parts[i]);
            }

            return accounts;
        }

        @Override
        public Account getAccount(String number) throws IOException {
            return new Account(number, in, out);
        }

        @Override
        public void transfer(bank.Account a, bank.Account b, double amount) throws IOException, IllegalArgumentException, OverdrawException, InactiveException {

            if (amount < 0) throw new IllegalArgumentException("amount is not allowed to be negative");
            if (!a.isActive() || !b.isActive())
                throw new InactiveException("one of the accounts involved is not active");
            if (a.getBalance() - amount < 0) throw new OverdrawException("balance not sufficient");

            sendCommand("transfer " + a.getNumber() + " " + b.getNumber() + " " + amount);
            String[] response = processResponse(receiveResponse());
            if ("true".equals(response[0])) {
            } else {
                throw new IOException("received false");
            }

        }

        public static String[] processResponse(String response) {
            response = response.strip();
            String[] strings = response.split(Pattern.quote(" "));
            return strings;
        }

        public void sendCommand(String command) throws IOException {
            out.writeUTF(command + "\r\n");
            out.flush();
        }

        public String receiveResponse() throws IOException {
            return in.readUTF();
        }

    }

    public static class Account implements bank.Account {

        String number;

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
            sendCommand("getOwner " + number);
            String response = receiveResponse();


            if ("error".equals(response)) {
                throw new IOException("received error");
            }

            return response;
        }

        @Override
        public boolean isActive() throws IOException {
            sendCommand("isActive " + number);
            String[] response = Bank.processResponse(receiveResponse());

            if ("error".equals(response[0])) {
                throw new IOException("received error");
            }

            return "true".equals(response[0]);
        }

        @Override
        public void deposit(double amount) throws IOException, IllegalArgumentException, InactiveException {

            if (amount < 0) throw new IllegalArgumentException("amount is not allowed to be negative");
            if (!isActive()) throw new InactiveException("this account is not active");

            sendCommand("deposit " + number + " " + amount);
            String[] response = Bank.processResponse(receiveResponse());

            if ("error".equals(response[0])) {
                throw new IOException("received error");
            }
        }

        @Override
        public void withdraw(double amount) throws IOException, IllegalArgumentException, OverdrawException, InactiveException {

            if (amount < 0) throw new IllegalArgumentException("amount is not allowed to be negative");
            if (!isActive()) throw new InactiveException("this account is not active");
            if (getBalance() - amount < 0) throw new OverdrawException("balance not sufficient");

            sendCommand("withdraw " + number + " " + amount);
            String[] response = Bank.processResponse(receiveResponse());

            if ("error".equals(response[0])) {
                throw new IOException("received error");
            }
        }

        @Override
        public double getBalance() throws IOException {
            sendCommand("getBalance " + number);
            String[] response = Bank.processResponse(receiveResponse());

            if ("error".equals(response[0])) {
                throw new IOException("received error");
            }
            return Double.parseDouble(response[0]);
        }


        public void sendCommand(String command) throws IOException {
            out.writeUTF(command + "\r\n");
            out.flush();
        }

        public String receiveResponse() throws IOException {
            return in.readUTF();
        }
    }

}
