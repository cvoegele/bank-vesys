package bank.network.UDP;

import bank.*;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Set;

public class Driver implements BankDriver {

    DatagramSocket socket;
    private Bank bank;

    String host;
    int port;

    @Override
    public void connect(String[] args) throws IOException {
        host = args[0];
        port = Integer.parseInt(args[1]);
        socket = new DatagramSocket();
        bank = new Bank(socket);
    }

    @Override
    public void disconnect() throws IOException {
        socket.close();
    }

    private static BankPackage sendRequest(DatagramSocket socket, BankPackage bankPackage) throws IOException {
        //i wish this code was more readable
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream ous = new ObjectOutputStream(bos);
            ous.writeObject(bankPackage);
            ous.close();
            ous.flush();
            byte[] buf = bos.toByteArray();

            var packet = new DatagramPacket(buf, buf.length,
                    InetAddress.getByName("localhost"), 4711);

            socket.send(packet);
            int packetsSent = 0;
            while (packetsSent < 5) {
                try {
                    socket.setSoTimeout(5000);
                    DatagramPacket responseShell = new DatagramPacket(new byte[100000], 100000);
                    socket.receive(responseShell); //will throw if no response

                    //received response
                    byte[] data = responseShell.getData();
                    ObjectInputStream bis = new ObjectInputStream(new ByteArrayInputStream(data));
                    Object obj = bis.readObject();

                    if (obj instanceof BankPackage) {
                        //check if response to this
                        if (bankPackage.isResponse((BankPackage) obj)) {
                            //correct response sent by bank
                            System.out.println(((BankPackage) obj).toString());
                            return (BankPackage) obj;
                        }
                    }

                } catch (SocketException | EOFException e) {
                    System.out.println("received timeout... sending again...");
                    e.printStackTrace();
                    socket.send(packet);
                    packetsSent++;
                }
            }
            throw new Exception("no response received after 5 timeouts");

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Did not receive response after 5 timeouts");
        }
    }

    @Override
    public Bank getBank() {
        return bank;
    }

    public static class Bank implements bank.Bank {

        DatagramSocket socket;

        public Bank(DatagramSocket socket) {
            this.socket = socket;
        }

        @Override
        public String createAccount(String owner) throws IOException {
            BankPackage bankPackage = new BankPackage("CREATEACCOUNT", owner, null, null);
            BankPackage response = sendRequest(socket, bankPackage);
            if (response.isOk()) {
                return response.getResponse();
            } else {
                if (response.getE() != null) throw (IOException) response.getE();
                throw new IOException(response.getResponse());
            }
        }

        @Override
        public boolean closeAccount(String number) throws IOException {
            BankPackage bankPackage = new BankPackage("CLOSEACCOUNT", number, null, null);
            BankPackage response = sendRequest(socket, bankPackage);
            if (response.isOk()) {
                return true;
            } else if (!response.isOk()) {
                return false;
            } else {
                if (response.getE() != null) throw (IOException) response.getE();
                throw new IOException("Did not receive useful information");
            }
        }

        @Override
        public Set<String> getAccountNumbers() throws IOException {
            BankPackage bankPackage = new BankPackage("GETACCOUNTS", null, null, null);
            BankPackage response = sendRequest(socket, bankPackage);
            if (response.isOk()) {
                return response.getAccountNumbers();
            } else {
                if (response.getE() != null) throw (IOException) response.getE();
                throw new IOException("Did not receive useful information");
            }
        }

        @Override
        public Account getAccount(String number) throws IOException {
            BankPackage bankPackage = new BankPackage("ISACCOUNT", number, null, null);
            BankPackage response = sendRequest(socket, bankPackage);
            if (response.isOk()) {
                return new Account(socket, number);
            } else {
                if (response.getE() != null) throw (IOException) response.getE();
                return null;
            }
        }

        @Override
        public void transfer(bank.Account a, bank.Account b, double amount) throws IOException, IllegalArgumentException, OverdrawException, InactiveException {
            BankPackage bankPackage = new BankPackage("TRANSFER", a.getNumber(), b.getNumber(), amount);
            BankPackage response = sendRequest(socket, bankPackage);
            if (!response.isOk() && response.getE() != null) {
                Exception e = response.getE();
                if (e instanceof IllegalArgumentException) throw (IllegalArgumentException) e;
                if (e instanceof OverdrawException) throw (OverdrawException) e;
                if (e instanceof InactiveException) throw (InactiveException) e;
            }
        }
    }

    public static class Account implements bank.Account {

        DatagramSocket socket;
        String number;

        public Account(DatagramSocket socket, String number) {
            this.socket = socket;
            this.number = number;
        }


        @Override
        public String getNumber() throws IOException {
            return number;
        }

        @Override
        public String getOwner() throws IOException {
            BankPackage bankPackage = new BankPackage("GETOWNER", number, null, null);
            BankPackage response = sendRequest(socket, bankPackage);
            if (response.isOk()) {
                return response.getResponse();
            } else {
                if (response.getE() != null) throw (IOException) response.getE();
                throw new IOException("Did not receive useful information");
            }
        }

        @Override
        public boolean isActive() throws IOException {
            BankPackage bankPackage = new BankPackage("ISACTIVE", number, null, null);
            BankPackage response = sendRequest(socket, bankPackage);
            if (response.isOk()) {
                return true;
            } else {
                if (response.getE() != null) throw (IOException) response.getE();
                return false;
            }
        }

        @Override
        public void deposit(double amount) throws IOException, IllegalArgumentException, InactiveException {
            BankPackage bankPackage = new BankPackage("DEPOSIT", number, null, amount);
            BankPackage response = sendRequest(socket, bankPackage);
            if (!response.isOk() && response.getE() != null) {
                Exception e = response.getE();
                if (e instanceof IllegalArgumentException) throw (IllegalArgumentException) e;
                if (e instanceof InactiveException) throw (InactiveException) e;
            }
        }

        @Override
        public void withdraw(double amount) throws IOException, IllegalArgumentException, OverdrawException, InactiveException {
            BankPackage bankPackage = new BankPackage("WITHDRAW", number, null, amount);
            BankPackage response = sendRequest(socket, bankPackage);
            if (!response.isOk() && response.getE() != null) {
                Exception e = response.getE();
                if (e instanceof IllegalArgumentException) throw (IllegalArgumentException) e;
                if (e instanceof OverdrawException) throw (OverdrawException) e;
                if (e instanceof InactiveException) throw (InactiveException) e;
            }
        }

        @Override
        public double getBalance() throws IOException {
            BankPackage bankPackage = new BankPackage("GETBALANCE", number, null, null);
            BankPackage response = sendRequest(socket, bankPackage);
            if (response.isOk()) {
                return Double.parseDouble(response.getResponse());
            } else {
                if (response.getE() != null) throw (IOException) response.getE();
                throw new IOException("Did not receive useful information");
            }
        }
    }

}
