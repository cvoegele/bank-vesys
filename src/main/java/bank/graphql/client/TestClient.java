package bank.graphql.client;

import bank.graphql.client.models.Root;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.URISyntaxException;

public class TestClient {

    public static void main(String[] args) throws URISyntaxException, IOException {
        var client = new GraphQLClient("http://localhost:8080/graphql");
        Gson gson = new Gson();
        var s = "OFS-8";

        try {
//            client.applyRequest("mutation {\n" +
//                    "  createAccount(owner: \"" + s + "\") {\n" +
//                    "    number\n" +
//                    "  }\n" +
//                    "}\n");
//            var jsonString = client.extractJsonBody();
//            Root root = gson.fromJson(jsonString, Root.class);
//            System.out.println(root.data.createAccount.number);
//            //System.out.println(response.data.accounts[0]);
            client.applyRequest("mutation { " +
                    "closeAccount(number: \"" + s + "\")" +
                    "}");
            var jsonString = client.extractJsonBody();
            var root = gson.fromJson(jsonString, Root.class);
            System.out.println(root.data.closeAccount);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
