import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

public class Application {

    public static void main(String[] args) throws IOException {
        WireMock.configureFor("localhost", 8443);

        stubFor(get("/product?productId=700110")
                .willReturn(ok()
                        .withHeader("Content-Type", "text/xml")
                        .withBody("{\n" +
                                "\t\"title\": \"title\",\n" +
                                "\t\"description\": \"description\",\n" +
                                "\t\"weight\": 3,\n" +
                                "\t\"height\": 0.1,\n" +
                                "\t\"length\": 0.1,\n" +
                                "\t\"width\": 0.1\n" +
                                "}")));

        stubFor(get("/price?productId=700110")
                .willReturn(ok()
                        .withHeader("Content-Type", "text/xml")
                        .withBody("{\n" +
                                "\t\"price\": 5,\n" +
                                "\t\"currency\": \"RUR\"\n" +
                                "}")));

        int serverPort = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(serverPort), 0);
        server.createContext("/v1/product", (exchange -> {

            if ("GET".equals(exchange.getRequestMethod())) {

                StringBuilder sb = new StringBuilder();
                InputStream ios = exchange.getRequestBody();
                int i;
                while ((i = ios.read()) != -1) {
                    sb.append((char) i);
                }

                ProductRequest productRequest = new Gson().fromJson(sb.toString(), ProductRequest.class);
                Map<String, String> queryParams = new HashMap<>();
                queryParams.put("productId", productRequest.getProductId());

                ProductInfo productInfo = new ProductInfo();
                ProductPrice productPrice = new ProductPrice();

                try {
                    HttpResponse<String> productInfoResponse = getRequest("/product", queryParams);
                    productInfo = new Gson().fromJson(productInfoResponse.body(), ProductInfo.class);
                    HttpResponse<String> productPriceResponse = getRequest("/price", queryParams);
                    productPrice = new Gson().fromJson(productPriceResponse.body(), ProductPrice.class);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }

                ProductResponse productResponse = new ProductResponse();
                productResponse.setTitle(productInfo.getTitle());
                productResponse.setPrice(productPrice.getPrice());
                productResponse.setCurrency(productPrice.getCurrency());

                switch (productRequest.getSource()) {
                    case DESKTOP:
                        productResponse.setDescription(productInfo.getDescription());
                        break;
                    case OFFLINE:
                        productResponse.setDescription(productInfo.getDescription());
                        productResponse.setWeight(productInfo.getWeight());
                        productResponse.setHeight(productInfo.getHeight());
                        productResponse.setLength(productInfo.getLength());
                        productResponse.setWidth(productInfo.getWidth());
                        break;
                }

                String response = new Gson().toJson(productResponse);

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream output = exchange.getResponseBody();
                output.write(response.getBytes());
                output.flush();
            } else {
                exchange.sendResponseHeaders(405, -1);// 405 Method Not Allowed
            }
            exchange.close();
        }));
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    private static HttpResponse<String> getRequest(String path, Map<String, String> params) throws IOException, InterruptedException, URISyntaxException {
        HttpClient client = HttpClient.newHttpClient();

        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("http").setHost("localhost").setPort(8443).setPath(path);

        for (Map.Entry<String, String> entry : params.entrySet()) {
            uriBuilder.setParameter(entry.getKey(), entry.getValue());
        }

        HttpRequest productsRequest = HttpRequest.newBuilder()
                .GET()
                .uri(uriBuilder.build())
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> productsResponse = client.send(productsRequest, HttpResponse.BodyHandlers.ofString());

        return productsResponse;
    }
}
