package bank.graphql.client;

import bank.graphql.client.models.Root;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GraphQLClient {

    private final HttpClient client;
    private final ObjectMapper objectMapper;
    private final Gson gson;

    private final URI serverUri;

    private HttpResponse<String> lastResponse;

    public GraphQLClient(String Uri) throws URISyntaxException {
        client = HttpClient.newHttpClient();
        objectMapper = new ObjectMapper();
        serverUri = new URI(Uri);
        gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void sendRequest(String query1) throws IOException {
        String requestBody = objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(new Query(query1));

        HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.ofString(requestBody);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(serverUri) //"http://localhost:8080/graphql"
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(body)
                .build();

        try {
            lastResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Root getJsonRoot() {
        return gson.fromJson(lastResponse.body(),Root.class);
    }

    static class Query {
        private final String query;
        private final String variables;

        public Query(String query, String variables) {
            this.query = query;
            this.variables = variables;
        }

        public Query(String query) {
            this(query, null);
        }

        public String getQuery() {
            return query;
        }

        public String getVariables() {
            return variables;
        }
    }
}
