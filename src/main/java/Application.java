import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import enums.Source;
import objects.ErrorResponse;
import objects.ProductInfo;
import objects.ProductPrice;
import objects.ProductRequest;
import objects.ProductResponse;
import objects.ProductStockInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpResponse;

import static clients.BackendClient.postRequest;

public class Application {

    public static void main(String[] args) throws IOException {

        int serverPort = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(serverPort), 0);
        server.createContext("/v1/product", (exchange -> {

            if ("POST".equals(exchange.getRequestMethod())) {

                int statusCode = 200;

                ProductRequest productRequest = processingRequest(exchange);

                ProductInfo productInfo = new ProductInfo();
                ProductPrice productPrice = new ProductPrice();
                ProductStockInfo productStockInfo = new ProductStockInfo();
                ErrorResponse errorResponse = null;

                HttpResponse<String> serviceResponse = null;

                try {
                    serviceResponse = postRequest("/info", productRequest.getProductId());

                    if (serviceResponse.statusCode() == 404) {
                        errorResponse = new ErrorResponse(107, "ProductInfo not found.");
                        statusCode = 404;
                    }

                    if (serviceResponse.statusCode() == 500) {
                        errorResponse = new ErrorResponse(101, "ProductInfo service error.");
                        statusCode = 500;
                    }

                } catch (IOException e) {
                    errorResponse = new ErrorResponse(101, "ProductInfo service error: " + e.getMessage());
                    statusCode = 500;
                }

                if (errorResponse == null) {
                    try {
                        productInfo = new Gson().fromJson(serviceResponse.body(), ProductInfo.class);
                    } catch (JsonParseException e) {
                        errorResponse = new ErrorResponse(104, "ProductInfo unexpected response.");
                        statusCode = 500;
                    }
                }

                try {
                    serviceResponse = postRequest("/price", productRequest.getProductId());

                    if (serviceResponse.statusCode() == 404) {
                        errorResponse = new ErrorResponse(108, "ProductPrice not found.");
                        statusCode = 404;
                    }

                    if (serviceResponse.statusCode() == 500) {
                        errorResponse = new ErrorResponse(102, "ProductPrice service error.");
                        statusCode = 500;
                    }

                } catch (IOException e) {
                    errorResponse = new ErrorResponse(102, "ProductPrice service error: " + e.getMessage());
                    statusCode = 500;
                }

                if (errorResponse == null) {
                    try {
                        productPrice = new Gson().fromJson(serviceResponse.body(), ProductPrice.class);
                    } catch (JsonParseException e) {
                        errorResponse = new ErrorResponse(105, "ProductPrice unexpected response.");
                        statusCode = 500;
                    }
                }

                if (productRequest.getSource() == Source.OFFLINE) {
                    try {
                        serviceResponse = postRequest("/stock", productRequest.getProductId());

                        if (serviceResponse.statusCode() == 404) {
                            errorResponse = new ErrorResponse(109, "ProductStock not found.");
                            statusCode = 404;
                        }
                        if (serviceResponse.statusCode() == 500) {
                            errorResponse = new ErrorResponse(103, "ProductStock service error.");
                            statusCode = 500;
                        }

                    } catch (JsonParseException e) {
                        errorResponse = new ErrorResponse(106, "ProductStock unexpected response.");
                        statusCode = 500;
                    } catch (IOException e) {
                        errorResponse = new ErrorResponse(103, "ProductStock service error: " + e.getMessage());
                        statusCode = 500;
                    }
                }

                if (errorResponse == null) {
                    try {
                        productStockInfo = new Gson().fromJson(serviceResponse.body(), ProductStockInfo.class);
                    } catch (JsonParseException e) {
                        errorResponse = new ErrorResponse(106, "ProductStock unexpected response.");
                        statusCode = 500;
                    }
                }

                String response = null;
                exchange.getResponseHeaders().set("Content-Type", "application/json");

                if (errorResponse != null) {
                    response = new Gson().toJson(errorResponse);
                } else {
                    ProductResponse productResponse = aggregateProductResponse(productRequest, productInfo, productPrice, productStockInfo);
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

    private static ProductRequest processingRequest(HttpExchange exchange) throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStream ios = exchange.getRequestBody();
        int i;
        while ((i = ios.read()) != -1) {
            sb.append((char) i);
        }

        System.out.println("Received request from client:");
        System.out.println(sb);
        ProductRequest productRequest = new Gson().fromJson(sb.toString(), ProductRequest.class);

        return productRequest;
    }

    private static ProductResponse aggregateProductResponse(ProductRequest productRequest, ProductInfo productInfo, ProductPrice productPrice, ProductStockInfo productStockInfo) {
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

        return productResponse;
    }
}
