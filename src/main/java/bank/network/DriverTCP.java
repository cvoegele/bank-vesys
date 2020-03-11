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
        // XXX ich würde hier noch die bank-Referenz auf null setzen.
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
                return response[1];	// XXX response[1] könnte auch "null" sein, dann müsste null zurückgegeben werden.
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
            	// XXX jetzt wird in JEDEM Schleifendurchgang Integer.parseInt(parts[0]) aufgerufen, das finde ich suboptimal
                accounts.add(parts[i]);
            }

            return accounts;
        }

        @Override
        public Account getAccount(String number) throws IOException {
            return new Account(number, in, out);
            // XXX fast, denn falls die Kontonummer nicht gültig ist dann sollte hier null zurückgegeben werden, also auf Server fragen ob Konto existiert.
        }

        @Override
        public void transfer(bank.Account a, bank.Account b, double amount) throws IOException, IllegalArgumentException, OverdrawException, InactiveException {

            if (amount < 0) throw new IllegalArgumentException("amount is not allowed to be negative");
            if (!a.isActive() || !b.isActive())
                throw new InactiveException("one of the accounts involved is not active");
            if (a.getBalance() - amount < 0) throw new OverdrawException("balance not sufficient");
            // XXX ok, das sind Optimierungen, aber auf dem Server könnte balance bereits geändert haben nach dieser Abfrage,
            //     daher sind diese zusätzlichen Round-Trips gar nicht sinnvoll (also keine wirkliche Optimierung)

            sendCommand("transfer " + a.getNumber() + " " + b.getNumber() + " " + amount);
            String[] response = processResponse(receiveResponse());
            if ("true".equals(response[0])) {
            } else {
                throw new IOException("received false");
                // XXX könnte auch eine OverdrawException oder eine  InactiveException sein, da erst nach obiger Abfrage das Konto auf Serverseite geändert worden ist.
            }

        }

        public static String[] processResponse(String response) {
            response = response.strip();
            return response.split(Pattern.quote(" "));
        }

        public void sendCommand(String command) throws IOException {
            out.writeUTF(command + "\r\n");	// XXX das \r\n wäre nicht nötig! Und vielleicht ist das auch der Fehler des Tests, denn ihre Namen haben nun 
            								//     zusätzlich noch ein \r\n am Ende.
            out.flush();	// XXX nicht nötig solange sie keinen BufferedStream verwenden, aber vielleicht wäre dies noch ein interessanter Test:
            				//     Zeitmessung mit und ohne BufferedStreams (und dann brauchen Sie das flush wieder....)
        }

        public String receiveResponse() throws IOException {
            return in.readUTF();
        }

    }

    public static class Account implements bank.Account {

        String number;	// XXX würde ich final deklarieren.

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

            System.out.println("received owner: " + response);
            if ("error".equals(response)) {
            	// XXX dann kann ich kein Konto für den Herrn "error" eröffnen ;-) 
            	//     Ah doch, denn der Kontoname ist dann "error\r\n" bzw. error\r\n\r\n
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
            // XXX diese Tests sollten auf Serverseite gemacht werden (werden sie auch wenn sie dann deposit xxx yyyy an den Server schicken,
            //     d.h. sie machen unnötige round-trips.

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
        	// XXX diese Methode kommt doppelt vor, aber anstelle des sendCommand können sie auch gerade out.writeUTF(command) aufrufen, denn
        	//     1. das Hinzufügen von \r\n macht keinen Sinn / braucht es nicht
        	//     2. das out.flush braucht es (aktuell) auch nicht.
            out.writeUTF(command + "\r\n"); // XXX dass Sie da \r\n anhängen macht keinen Sinn.
            out.flush();
        }

        public String receiveResponse() throws IOException {
            return in.readUTF();
        }
    }

}
