package bank.network;

import bank.Account;
import bank.Bank;
import bank.InactiveException;
import bank.OverdrawException;
import bank.local.Driver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BankServer {


    public static void main(String[] args) {
        startServer();
    }

    private static Driver.Bank bank = new Driver.Bank();

    public static void startServer() {
        int port = 1234;
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Startet Echo Server on port " + port);
            while (true) {
                Socket s = server.accept();
                Thread t = new Thread(() -> {
                    try {
                        while (true) {
                            //process Input
                            DataInputStream inputStream = new DataInputStream(s.getInputStream());
                            DataOutputStream outputStream = new DataOutputStream(s.getOutputStream());
                            String response = processInput(inputStream.readUTF());
                            if (!response.equals("")) {
                                //send response
                                outputStream.writeUTF(response + "\r\n");
                                outputStream.flush();
                                System.out.println("sent response" + response);
                            }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("done serving " + s.isClosed());
                });
                t.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String processInput(String input) {
        System.out.println("received command " + input);
        String[] arguments = input.split("\\s+");
        if (arguments.length == 0) return "bad command sent";
        String command = arguments[0];

        switch (command) {
            case "accounts":
                System.out.println("sending account numbers");
                return getAccountNumbers();
            case "createAccount":
                return createAccount(input.substring(14));
            case "closeAccount":
                return closeAccount(arguments[1]);
            case "transfer":
                return transfer(arguments[1], arguments[2], Double.parseDouble(arguments[3]));
            case "getBalance":
                return getBalance(arguments[1]);
            case "getOwner":
                return getOwner(arguments[1]);
            case "isActive":
                return isActive(arguments[1]);
            case "deposit":
                return deposit(arguments[1], Double.parseDouble(arguments[2]));
            case "withdraw":
                return withdraw(arguments[1], Double.parseDouble(arguments[2]));

        }
        return "";
    }

    //API Interface Definition
    //Bank
    //getAccountNumbers - none - command: accounts, result: n "acc N" "acc N" "acc N"
    private static String getAccountNumbers() {
        Set<String> accounts = bank.getAccountNumbers();
        if (accounts.size() == 0) return "0";
        StringBuilder output = new StringBuilder();
        output.append(accounts.size());
        output.append(" ");
        for (String account : accounts) {
            output.append(account);
            output.append(" ");
        }
        return output.toString();
    }

    //createAccount - name - command: createAccount, result: true "account Number" or false
    private static String createAccount(String name) {
        String accountNumber = bank.createAccount(name);
        return "true " + accountNumber;
    }

    //closeAccount - number - command: closeAccount, result: true or false
    private static String closeAccount(String number) {
        boolean isClosed = bank.closeAccount(number);
        return isClosed ? "true" : "false";
    }

    //transfer - numberFrom, numberTo, amount - command: transfer "" "" int, result : true or false
    private static String transfer(String from, String to, double amount) {
        try {
            transfer(bank.getAccount(from).getNumber(), bank.getAccount(to).getNumber(), amount);
        } catch (Exception e) {
            return "false";
        }
        return "true";
    }

    //getBalance - number - command: getBalance "", result: int
    private static String getBalance(String number) {
        try {
            return String.valueOf(bank.getAccount(number).getBalance());
        } catch (IOException e) {
            return "error";
        }
    }

    //getOwner - number - command: getOwner "", result: ""
    private static String getOwner(String number) {
        try {
            return bank.getAccount(number).getOwner();
        } catch (IOException e) {
            return "error";
        }
    }

    //isActive - number - command: isActive "", result: true or false
    private static String isActive(String number) {
        try {
            return bank.getAccount(number).isActive() ? "true" : "false";
        } catch (IOException e) {
            return "error";
        }
    }

    //deposit - number amount - command: deposit "" int, result: true or false
    private static String deposit(String number, double amount) {
        try {
            bank.getAccount(number).deposit(amount);
            return "true";
        } catch (IOException | InactiveException e) {
            return "false";
        }
    }

    //withdraw - number amount - command: withdraw "" int, result: true or false
    private static String withdraw(String number, double amount) {
        try {
            bank.getAccount(number).withdraw(amount);
            return "true";
        } catch (OverdrawException | InactiveException | IOException e) {
            return "false";
        }
    }


}
