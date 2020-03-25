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
            DatagramPacket requestPacket = new DatagramPacket(new byte[PACKAGE_SIZE], PACKAGE_SIZE);

            while (true) {
                socket.receive(requestPacket);

                InetAddress address = requestPacket.getAddress();
                int port = requestPacket.getPort();
                int len = requestPacket.getLength();
                int offset = requestPacket.getOffset(); // XXX wird gar nicht verwendet.
                byte[] data = requestPacket.getData();
                
                // XXX nach dem Aufruf ist data.length immer 1000000, aber das spielt keine Rolle da einfach die nötigen Daten bei readObject ausgelesen werden.
                //     getData liefert einfach die Referenz auf den byte[], aber das ist OK so!

                //read request object
                ObjectInputStream bis = new ObjectInputStream(new ByteArrayInputStream(data));
                Object obj = bis.readObject();

                //check if request valid
                if (obj instanceof BankPackage) {
                    BankPackage bankPackage = (BankPackage) obj;

                    //received requests is new --> still needs to be handled
                    if (!requests.contains(bankPackage)) {
                        requests.add(bankPackage); // XXX ok, und auf diesem Objekt wird dann die Antwort geschrieben.
                        BankPackage responsePackage = processRequest(bankPackage);
                        // XXX das processRequest gefällt mir von der SIgnatur her nicht, d.h. es sieht so aus als ob ein (neues?) Objekt zurückkommt, und daher
                        //     hätte ich erwartet dass die Response irgendwo gespeichert wird. Aber da bankPackage und responsePackage auf dasselbe Objekt zeigen
                        //     läuft alles richtig, aber das sieht man der Schnittstelle nicht an.

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
                        DatagramPacket responsePacket = new DatagramPacket(bufResponse, bufResponse.length, address, port);

                        //send response
                        System.out.println("sent response" + responsePackage.toString());
                        socket.send(responsePacket);
                    } else {
                        //we need to send the conformation again
                        BankPackage responsePackage = new BankPackage(bankPackage.getCommand(),bankPackage.getAccount(),bankPackage.getCounterAccount(),bankPackage.getAmount()); // XXX da würde sich ein copy-Konstruktor aufdrängen/anbieten.
                        responsePackage.setDate(bankPackage.getDate()); //important for identification
                        // XXX Frage: Warum ein neues BankPackage? Sie könnten ja einfach nochmals den in der Liste gespeicherten Request zurückschicken, der 
                        //     enthält ja die Antwort. Ws sie jetzt zurückschicken ist die Anfrage ohne die Antwort
                        // XXX vom code her ist das Schicken identisch. Es genügt wenn Sie oben die Antwort bereitstellen (entweder berechnen oder auslesen) und diese dann mit demselben COde zurückschicken.
                        
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ObjectOutputStream ous = new ObjectOutputStream(bos);
                        ous.writeObject(responsePackage);
                        ous.close();
                        ous.flush();
                        byte[] bufResponse = bos.toByteArray();
                        DatagramPacket responsePacket = new DatagramPacket(bufResponse, bufResponse.length, address, port);

                        //send response
                        System.out.println("sent second response" + responsePackage.toString());
                        socket.send(responsePacket);
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
                	// XXX gut, damit wird auch eine mögliche NPE abgefangen.
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
                    double balance = localBank.getAccount(request.getAccount()).getBalance(); // XXX das könnte eine NPE werfen die NICHT abgefangen wird! Bei getOwner haben sie diesen Fall explizit abgefangen.
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
                    boolean active = localBank.getAccount(request.getAccount()).isActive(); // XXX gefahr NPE
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
                    request.setStatus(true); // XXX :-) ich würde den Status nach dem transfer auf true setzen.
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
