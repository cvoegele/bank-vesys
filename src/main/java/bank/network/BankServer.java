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
                        while (true) {
                            //process Input
                            DataInputStream inputStream = new DataInputStream(s.getInputStream());
                            DataOutputStream outputStream = new DataOutputStream(s.getOutputStream());
                            // XXX ich würde die beiden STreams ausserhalb der Schleife erzeugen und immer dieselben Instanzen verwenden,
                            //     aber bezüglcih Funktionalität spielt dies keine Rolle
//                            if (inputStream.available() <= 0) { // XXX dazu habe ich mich bereits geäussert.
                            String response = processInput(inputStream.readUTF());
                            if (!response.equals("")) {
                                //send response
                                outputStream.writeUTF(response);
                                System.out.println("sent response: " + response);
                            } // XXX und sonst? Da könnte man allenfalls abbrechen wenn ein leerer String kommt und dies als close interpretieren.
//                            }
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
                return createAccount(input.substring(14)); // XXX statt 14 würde ich hier vielleicht "createAccount".size()+1 schreiben.
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
        if (accounts.size() == 0)
            return "0"; // XXX dieser Spezialfall wäre nicht nötig, d.h. das wird auch vom code unten richtig behandelt.
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
        return "true " + accountNumber; // XXX könnte auch "true null" sein! Müsste auf Klientenseite entsprechend berücksichtigt werden.
    }

    //closeAccount - number - command: closeAccount, result: true or false
    private static String closeAccount(String number) {
        boolean isClosed = bank.closeAccount(number);     // XXX Variante: return "" + bank.closeAccount(number);
        return isClosed ? "true" : "false";
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
            return "false";    // XXX hier sollte man aber noch zurückgeben welche Exception geworfen worden ist
        }
        return "true";
    }

    //getBalance - number - command: getBalance "", result: int
    private static String getBalance(String number) {
        try {
            return String.valueOf(bank.getAccount(number).getBalance());    // XXX falls bank.getAccount(number) null ist dann führt dies zu einer NPE
            //     gilt analog bei allen folgnedne Methoden auch.
        } catch (IOException e) {
            return "error";    // XXX anstelle von return "error"  könnten sie auch auf Server-Seite eine RuntimeException werfen, denn das sollte ja
            //     (mit der lokalen Bank) nicht vorkommen.
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
            return "false";    // XXX auch hier: Die Exception sollte zurückgegeben werden, z.B. return e.getType() oder so.
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
