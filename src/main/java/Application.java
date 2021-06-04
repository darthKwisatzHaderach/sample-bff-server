import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Application {

    public static void main(String[] args) throws IOException {

        int serverPort = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(serverPort), 0);
        server.createContext("/v1/product", (exchange -> {

            if ("POST".equals(exchange.getRequestMethod())) {

                int statusCode = 200;

                StringBuilder sb = new StringBuilder();
                InputStream ios = exchange.getRequestBody();
                int i;
                while ((i = ios.read()) != -1) {
                    sb.append((char) i);
                }

                System.out.println(sb);
                ProductRequest productRequest = new Gson().fromJson(sb.toString(), ProductRequest.class);

                ProductInfo productInfo = new ProductInfo();
                ProductPrice productPrice = new ProductPrice();
                ProductStockInfo productStockInfo = new ProductStockInfo();
                ErrorResponse errorResponse = null;

                try {
                    HttpResponse<String> productInfoResponse = postRequest("/info", productRequest.getProductId());
                    productInfo = new Gson().fromJson(productInfoResponse.body(), ProductInfo.class);

                    if (productInfoResponse.statusCode() == 404) {
                        errorResponse = new ErrorResponse(107, "ProductInfo not found.");
                        statusCode = 404;
                    }

                    if (productInfoResponse.statusCode() == 500) {
                        errorResponse = new ErrorResponse(101, "ProductInfo service error.");
                        statusCode = 500;
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                } catch (JsonParseException e) {
                    errorResponse = new ErrorResponse(104, "ProductInfo unexpected response: " + e.getMessage());
                    statusCode = 500;
                } catch (IOException e) {
                    errorResponse = new ErrorResponse(101, "ProductInfo service error: " + e.getMessage());
                    statusCode = 500;
                }

                try {
                    HttpResponse<String> productPriceResponse = postRequest("/price", productRequest.getProductId());
                    productPrice = new Gson().fromJson(productPriceResponse.body(), ProductPrice.class);

                    if (productPriceResponse.statusCode() == 404) {
                        errorResponse = new ErrorResponse(108, "ProductPrice not found.");
                        statusCode = 404;
                    }

                    if (productPriceResponse.statusCode() == 500) {
                        errorResponse = new ErrorResponse(102, "ProductPrice service error.");
                        statusCode = 500;
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                } catch (JsonParseException e) {
                    errorResponse = new ErrorResponse(105, "ProductPrice unexpected response: " + e.getMessage());
                    statusCode = 500;
                } catch (IOException e) {
                    errorResponse = new ErrorResponse(102, "ProductPrice service error: " + e.getMessage());
                    statusCode = 500;
                }

                if (productRequest.getSource() == Source.OFFLINE) {
                    try {
                        HttpResponse<String> productStockInfoResponse = postRequest("/stock", productRequest.getProductId());
                        productStockInfo = new Gson().fromJson(productStockInfoResponse.body(), ProductStockInfo.class);

                        if (productStockInfoResponse.statusCode() == 404) {
                            errorResponse = new ErrorResponse(109, "ProductStock not found.");
                            statusCode = 404;
                        }
                        if (productStockInfoResponse.statusCode() == 500) {
                            errorResponse = new ErrorResponse(103, "ProductStock service error.");
                            statusCode = 500;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    } catch (JsonParseException e) {
                        errorResponse = new ErrorResponse(106, "ProductStock unexpected response: " + e.getMessage());
                        statusCode = 500;
                    } catch (IOException e) {
                        errorResponse = new ErrorResponse(103, "ProductStock service error: " + e.getMessage());
                        statusCode = 500;
                    }
                }

                String response = null;
                exchange.getResponseHeaders().set("Content-Type", "application/json");

                if (errorResponse != null) {
                    response = new Gson().toJson(errorResponse);
                } else {
                    ProductResponse productResponse = new ProductResponse();
                    productResponse.setTitle(productInfo.getTitle());
                    productResponse.setPrice(productPrice.getPrice());
                    productResponse.setCurrency(productPrice.getCurrency());

                    if (productRequest.getSource() != Source.MOBILE) {
                        productResponse.setDescription(productInfo.getDescription());
                        productResponse.setWeight(productInfo.getWeight());
                        productResponse.setHeight(productInfo.getHeight());
                        productResponse.setLength(productInfo.getLength());
                        productResponse.setWidth(productInfo.getWidth());
                    }

                    if (productRequest.getSource() == Source.OFFLINE) {
                        productResponse.setAvailableStock(productStockInfo.getAvailableStock());
                        productResponse.setRow(productStockInfo.getRow());
                        productResponse.setShell(productStockInfo.getShell());
                    }

                    response = new Gson().toJson(productResponse);
                }

                exchange.sendResponseHeaders(statusCode, response.getBytes().length);

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

    private static HttpResponse<String> postRequest(String path, Object body) throws IOException, InterruptedException, URISyntaxException {
        Gson gson = new Gson();
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest productsRequest = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .uri(URI.create("http://localhost:8443" + path))
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> productsResponse = client.send(productsRequest, HttpResponse.BodyHandlers.ofString());

        return productsResponse;
    }
}
