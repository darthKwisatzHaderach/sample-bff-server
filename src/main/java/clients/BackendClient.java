package clients;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class BackendClient {

    private static final String baseUrl = "http://localhost:8443";

    public static HttpResponse<String> postRequest(String path, Object body) throws IOException {
        Gson gson = new Gson();
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest productsRequest = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .uri(URI.create(baseUrl + path))
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = null;

        try {
            response = client.send(productsRequest, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println(String.format("Received response from %s:", path));
        System.out.println(response);
        System.out.println(response.body());

        return response;
    }
}
