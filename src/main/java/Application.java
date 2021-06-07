import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import enums.Source;
import objects.requests.ProductInfoRequest;
import objects.requests.ProductPriceRequest;
import objects.requests.ProductRequest;
import objects.requests.ProductStockInfoRequests;
import objects.responses.ErrorResponse;
import objects.responses.ProductInfoResponse;
import objects.responses.ProductPriceResponse;
import objects.responses.ProductResponse;
import objects.responses.ProductStockInfoResponse;

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

                ProductInfoResponse productInfoResponse = new ProductInfoResponse();
                ProductPriceResponse productPriceResponse = new ProductPriceResponse();
                ProductStockInfoResponse productStockInfoResponse = new ProductStockInfoResponse();
                ErrorResponse errorResponse = null;

                HttpResponse<String> serviceResponse = null;

                ProductInfoRequest productInfoRequest = new ProductInfoRequest(productRequest.getProductId());

                try {
                    serviceResponse = postRequest("/info", productInfoRequest);

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
                        productInfoResponse = new Gson().fromJson(serviceResponse.body(), ProductInfoResponse.class);
                    } catch (JsonParseException e) {
                        errorResponse = new ErrorResponse(104, "ProductInfo unexpected response.");
                        statusCode = 500;
                    }
                }

                ProductPriceRequest productPriceRequest = new ProductPriceRequest(productRequest.getProductId());

                try {
                    serviceResponse = postRequest("/price", productPriceRequest);

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
                        productPriceResponse = new Gson().fromJson(serviceResponse.body(), ProductPriceResponse.class);
                    } catch (JsonParseException e) {
                        errorResponse = new ErrorResponse(105, "ProductPrice unexpected response.");
                        statusCode = 500;
                    }
                }

                if (productRequest.getSource() == Source.OFFLINE) {

                    ProductStockInfoRequests productStockInfoRequests = new ProductStockInfoRequests(productRequest.getProductId());

                    try {
                        serviceResponse = postRequest("/stock", productStockInfoRequests);

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
                        productStockInfoResponse = new Gson().fromJson(serviceResponse.body(), ProductStockInfoResponse.class);
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
                    ProductResponse productResponse = aggregateProductResponse(productRequest, productInfoResponse, productPriceResponse, productStockInfoResponse);
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

    private static ProductResponse aggregateProductResponse(ProductRequest productRequest, ProductInfoResponse productInfoResponse, ProductPriceResponse productPriceResponse, ProductStockInfoResponse productStockInfoResponse) {
        ProductResponse productResponse = new ProductResponse();
        productResponse.setTitle(productInfoResponse.getTitle());
        productResponse.setPrice(productPriceResponse.getPrice());
        productResponse.setCurrency(productPriceResponse.getCurrency());

        if (productRequest.getSource() != Source.MOBILE) {
            productResponse.setDescription(productInfoResponse.getDescription());
            productResponse.setWeight(productInfoResponse.getWeight());
            productResponse.setHeight(productInfoResponse.getHeight());
            productResponse.setLength(productInfoResponse.getLength());
            productResponse.setWidth(productInfoResponse.getWidth());
        }

        if (productRequest.getSource() == Source.OFFLINE) {
            productResponse.setAvailableStock(productStockInfoResponse.getAvailableStock());
            productResponse.setRow(productStockInfoResponse.getRow());
            productResponse.setShell(productStockInfoResponse.getShell());
        }

        return productResponse;
    }
}
