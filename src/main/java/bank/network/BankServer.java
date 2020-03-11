package bank.network;

import bank.Account;
import bank.InactiveException;
import bank.OverdrawException;
import bank.local.Driver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;

public class BankServer {


    public static void main(String[] args) {
        startServer();
    }

    private static Driver.Bank bank = new Driver.Bank();

    public static void startServer() {
        int port = 1234;
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Startet Bank Server on port " + port);
            while (true) {
                Socket s = server.accept();
                Thread t = new Thread(() -> {
                    try {
                        DataInputStream inputStream = new DataInputStream(s.getInputStream());
                        DataOutputStream outputStream = new DataOutputStream(s.getOutputStream());
                        while (true) {
                            String response = processInput(inputStream.readUTF());
                            if (!response.equals("")) {
                                //send response
                                outputStream.writeUTF(response);
                                System.out.println("sent response: " + response);
                            } else {
                                s.close();
                            }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                });
                t.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String processInput(String input) {
        System.out.println("received command: " + input);
        String[] arguments = input.split("\\s+");
        if (arguments.length == 0) return "bad command sent";
        String command = arguments[0];

        switch (command) {
            case "accounts":
                System.out.println("sending account numbers");
                return getAccountNumbers();
            case "createAccount":
                return createAccount(input.substring("createAccount".length() + 1));
            case "closeAccount":
                return closeAccount(arguments[1]);
            case "isAccount":
                String arg = arguments.length == 1 ? "" : arguments[1];
                return isAccount(arg);
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
        return accountNumber != null ? "true " + accountNumber : "false";
    }

    //closeAccount - number - command: closeAccount, result: true or false
    private static String closeAccount(String number) {
        return "" + bank.closeAccount(number);
    }

    //isAccount (exist) - number - command: isAccount, result true or false
    private static String isAccount(String number) {
        Account account = bank.getAccount(number);
        if (account == null) return "false";
        return "true";
    }

    //transfer - numberFrom, numberTo, amount - command: transfer "" "" int, result : true or false
    private static String transfer(String from, String to, double amount) {
        try {
            bank.transfer(bank.getAccount(from), bank.getAccount(to), amount);
        } catch (Exception e) {
            return e.getClass().toString();
        }
        return "true";
    }

    //getBalance - number - command: getBalance "", result: int
    private static String getBalance(String number) {
        try {

            Account acc = bank.getAccount(number);
            if (acc != null)
                return String.valueOf(acc.getBalance());
            return "Account not present";

        } catch (Exception e) {
            return e.toString();
        }
    }

    //getOwner - number - command: getOwner "", result: ""
    private static String getOwner(String number) {
        try {
            Account acc = bank.getAccount(number);
            if (acc != null)
                return acc.getOwner();
            return "Account not present";
        } catch (IOException e) {
            return e.getClass().toString();
        }
    }

    //isActive - number - command: isActive "", result: true or false
    private static String isActive(String number) {
        try {
            Account acc = bank.getAccount(number);
            if (acc != null)
                return acc.isActive() ? "true" : "false";
            return "Account not present";
        } catch (IOException e) {
            return e.toString();
        }
    }

    //deposit - number amount - command: deposit "" int, result: true or false
    private static String deposit(String number, double amount) {
        try {
            Account acc = bank.getAccount(number);
            if (acc != null) {
                acc.deposit(amount);
                return "true";
            } else {
                return "false";
            }

        } catch (IOException | InactiveException e) {
            return e.getClass().toString();
        }
    }

    //withdraw - number amount - command: withdraw "" int, result: true or false
    private static String withdraw(String number, double amount) {
        try {
            Account acc = bank.getAccount(number);
            if (acc != null) {
                acc.withdraw(amount);
                return "true";
            } else {
                return "false";
            }
        } catch (OverdrawException | InactiveException | IOException e) {
            return e.getClass().toString();
        }
    }


}
