package bank.graphql.client;

import bank.*;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

public class GraphQLDriver implements BankDriver {
    private final Bank bank = new Bank();
    private static GraphQLClient client;
    private static Gson gson;

    @Override
    public void connect(String[] args) throws IOException {
        var host = args[0];
        var port = Integer.parseInt(args[1]);
        try {
            client = new GraphQLClient("http://" + host + ":" + port + "/graphql");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        gson = new Gson();
    }

    @Override
    public void disconnect() throws IOException {

    }

    @Override
    public Bank getBank() {
        return bank;
    }

    private static class Bank implements bank.Bank {


        @Override
        public String createAccount(String s) throws IOException {
            client.sendRequest("mutation {\n" +
                    "  createAccount(owner: \"" + s + "\") {\n" +
                    "    number\n" +
                    "  }\n" +
                    "}\n");
            var root = client.getJsonRoot();
            return root.data.createAccount.number;
        }

        @Override
        public boolean closeAccount(String s) throws IOException {
            client.sendRequest("mutation { " +
                    "closeAccount(number: \"" + s + "\")" +
                    "}");
            var root = client.getJsonRoot();
            return root.data.closeAccount;
        }

        @Override
        public Set<String> getAccountNumbers() throws IOException {
            client.sendRequest("query {" +
                    "accounts {" +
                    "number" +
                    "}" +
                    "}");
            var root = client.getJsonRoot();
            //change datatype to Set<String>
            var result = new HashSet<String>();
            for (var number : root.data.accounts) {
                result.add(number.number);
            }
            return result;
        }

        @Override
        public Account getAccount(String s) throws IOException {
            client.sendRequest("{ account(number: \"" + s + "\") {" +
                    "owner" +
                    "}" +
                    "}");
            var root = client.getJsonRoot();
            if (root.data.account == null) return null;
            return new Account(s);
        }

        @Override
        public void transfer(bank.Account account, bank.Account account1, double v) throws IOException, IllegalArgumentException, OverdrawException, InactiveException {

            if (v < 0) throw new IllegalArgumentException("amount is not allowed to be negative");
            //idk why this is needed but without this line it always complains about missing exception, even though I pass all exceptions to the client
            if (!account.isActive() || !account1.isActive())
                throw new InactiveException("One of the accounts is not active");

            client.sendRequest("mutation { " +
                    "transfer(from: \"" + account.getNumber() + "\", to: \"" + account1.getNumber() + "\" , amount: " + v + ")" +
                    "}");
            var root = client.getJsonRoot();

            if ("bank.InactiveException".equals(root.data.deposit)) {
                throw new InactiveException("account is not active");
            }
            if ("bank.OverdrawException".equals(root.data.deposit)) {
                throw new OverdrawException("funds are not sufficient");
            }
        }

    }

    private static class Account implements bank.Account {

        private final String number;

        public Account(String number) {
            this.number = number;
        }

        @Override
        public String getNumber() throws IOException {
            return number;
        }

        @Override
        public String getOwner() throws IOException {
            client.sendRequest("{ account(number: \"" + number + "\") {" +
                    "owner" +
                    "}" +
                    "}");
            var root = client.getJsonRoot();
            return root.data.account.getOwner();
        }

        @Override
        public boolean isActive() throws IOException {
            client.sendRequest("{ account(number: \"" + number + "\") {" +
                    "active" +
                    "}" +
                    "}");
            var root = client.getJsonRoot();
            return root.data.account.getActive();
        }

        @Override
        public void deposit(double v) throws IOException, IllegalArgumentException, InactiveException {

            if (v < 0) throw new IllegalArgumentException("amounts are not allowed to be negative");

            client.sendRequest("mutation { " +
                    "deposit(number: \"" + number + "\", amount: " + v + ")" +
                    "}");
            var root = client.getJsonRoot();

            if ("bank.InactiveException".equals(root.data.deposit)) {
                throw new InactiveException("account is not active");
            }
        }

        @Override
        public void withdraw(double v) throws IOException, IllegalArgumentException, OverdrawException, InactiveException {

            if (v < 0) throw new IllegalArgumentException("amounts are not allowed to be negative");

            client.sendRequest("mutation { " +
                    "withdraw(number: \"" + number + "\", amount: " + v + ")" +
                    "}");
            var root = client.getJsonRoot();

            if ("bank.InactiveException".equals(root.data.deposit)) {
                throw new InactiveException("account is not active");
            }
            if ("bank.OverdrawException".equals(root.data.deposit)) {
                throw new OverdrawException("funds are not sufficient");
            }
        }

        @Override
        public double getBalance() throws IOException {
            client.sendRequest("{ account(number: \"" + number + "\") {" +
                    "balance" +
                    "}" +
                    "}");
            var root = client.getJsonRoot();
            return root.data.account.getBalance();
        }
    }
}
