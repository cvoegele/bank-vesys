package bank.network.UDP;

import bank.Account;
import bank.local.Driver;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;

public class Server {

    private static final int PACKAGE_SIZE = 1_000_000;
    private static Driver.Bank localBank;

    private static ArrayList<BankPackage> requests;

    public static void main(String[] args) throws Exception {

        requests = new ArrayList<>();
        localBank = new Driver.Bank();

        try (DatagramSocket socket = new DatagramSocket(4711)) {
            System.out.println(socket.getLocalAddress());
            System.out.println(socket.getLocalPort());
            DatagramPacket packet = new DatagramPacket(new byte[PACKAGE_SIZE], PACKAGE_SIZE);

            while (true) {
                socket.receive(packet);

                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                int len = packet.getLength();
                int offset = packet.getOffset();
                byte[] data = packet.getData();

                //read request object
                ObjectInputStream bis = new ObjectInputStream(new ByteArrayInputStream(data));
                Object obj = bis.readObject();

                //check if request valid
                if (obj instanceof BankPackage) {
                    BankPackage bankPackage = (BankPackage) obj;

                    //received requests is new --> still needs to be handled
                    if (!requests.contains(bankPackage)) {
                        requests.add(bankPackage);
                        BankPackage responsePackage = processRequest(bankPackage);

                        //send response
                        //increment Response Counter
                        responsePackage.incrementRes();
                        //create
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ObjectOutputStream ous = new ObjectOutputStream(bos);
                        ous.writeObject(responsePackage);
                        ous.close();
                        ous.flush();
                        byte[] bufResponse = bos.toByteArray();
                        DatagramPacket response = new DatagramPacket(bufResponse, bufResponse.length, address, port);

                        //set new content to package
                   /* packet.setAddress(address);
                    packet.setData(buf);*/
                        //packet.setLength(buf.length);

                        //send response
                        System.out.println("sent response" + responsePackage.toString());
                        socket.send(response);
                    }
                }

                System.out.printf("Request from %s:%d of length %d: %s%n", address, port, len,
                        new String(data, 0, len, StandardCharsets.US_ASCII));
            }
        }
    }

    private static BankPackage processRequest(BankPackage request) {
        switch (request.getCommand()) {

            case "CLOSEACCOUNT":
                request.setStatus(localBank.closeAccount(request.getAccount()));
                break;
            case "CREATEACCOUNT":
                String number = localBank.createAccount(request.getAccount());
                request.setStatus(true);
                request.setResponse(number);
                break;
            case "DEPOSIT":
                try {
                    request.setStatus(true);
                    localBank.getAccount(request.getAccount()).deposit(request.getAmount());
                } catch (Exception e) {
                    request.setStatus(false);
                    request.setE(e);
                }
                break;
            case "GETACCOUNTS":
                HashSet<String> numbers = (HashSet<String>) localBank.getAccountNumbers();
                request.setAccountNumbers(numbers);
                request.setStatus(true);
                break;
            case "GETBALANCE":
                try {
                    double balance = localBank.getAccount(request.getAccount()).getBalance();
                    request.setResponse(String.valueOf(balance));
                    request.setStatus(true);
                } catch (IOException e) {
                    request.setStatus(false);
                    request.setE(e);
                }
                break;
            case "GETOWNER":
                try {
                    Account account = localBank.getAccount(request.getAccount());
                    if (account != null) {
                        String owner = account.getOwner();
                        request.setResponse(owner);
                        request.setStatus(true);
                    } else {
                        request.setStatus(false);
                    }
                } catch (IOException e) {
                    request.setStatus(false);
                    request.setE(e);
                }
                break;
            case "ISACCOUNT":
                request.setStatus(localBank.getAccount(request.getAccount()) != null);
                break;
            case "ISACTIVE":
                try {
                    boolean active = localBank.getAccount(request.getAccount()).isActive();
                    request.setStatus(active);
                } catch (IOException e) {
                    request.setStatus(false);
                    request.setE(e);
                }
                break;
            case "TRANSFER":
                Account account = localBank.getAccount(request.getAccount());
                Account counterAccount = localBank.getAccount(request.getCounterAccount());
                try {
                    request.setStatus(true);
                    localBank.transfer(account, counterAccount, request.getAmount());
                } catch (Exception e) {
                    request.setStatus(false);
                    request.setE(e);
                }
                break;
            case "WITHDRAW":
                try {
                    request.setStatus(true);
                    localBank.getAccount(request.getAccount()).withdraw(request.getAmount());
                } catch (Exception e) {
                    request.setStatus(false);
                    request.setE(e);
                }
                break;
        }
        return request;
    }

}
