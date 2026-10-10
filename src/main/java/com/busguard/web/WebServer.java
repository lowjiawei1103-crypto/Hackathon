package com.busguard.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class WebServer {

    private int port;
    private final ApiController controller;
    private HttpServer server;

    public WebServer(int port, ApiController controller) {
        this.port = port;
        this.controller = controller;
    }

    public int start() throws IOException {
        int targetPort = this.port;
        int maxAttempts = 10;
        
        for (int i = 0; i < maxAttempts; i++) {
            try {
                server = HttpServer.create(new InetSocketAddress(targetPort), 0);
                this.port = targetPort;
                break;
            } catch (BindException e) {
                targetPort++;
            }
        }

        if (server == null) {
            throw new IOException("Could not bind HTTP server on ports " + port + " through " + targetPort);
        }

        // API Handler
        server.createContext("/api", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String path = exchange.getRequestURI().getPath();
                String query = exchange.getRequestURI().getQuery();
                String method = exchange.getRequestMethod();

                String body = "";
                if ("POST".equalsIgnoreCase(method)) {
                    InputStream is = exchange.getRequestBody();
                    body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }

                // Enable CORS
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                exchange.getResponseHeaders().set("Content-Type", "application/json");

                if ("OPTIONS".equalsIgnoreCase(method)) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                if (path.equals("/api/download-dataset")) {
                    String fileType = getQueryParam(query, "type");
                    String filePath = "data/rapid_kl_bus_trips.csv";
                    String filename = "rapid_kl_bus_trips.csv";
                    if ("10k".equalsIgnoreCase(fileType) || "synthetic".equalsIgnoreCase(fileType)) {
                        filePath = "data/rapid_kl_10k_synthetic.csv";
                        filename = "rapid_kl_10k_synthetic.csv";
                    }

                    File downloadFile = new File(filePath);
                    if (!downloadFile.exists()) {
                        filePath = "data/rapid_kl_bus_trips.csv";
                        downloadFile = new File(filePath);
                    }

                    exchange.getResponseHeaders().set("Content-Type", "text/csv");
                    exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + filename + "\"");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

                    byte[] fileBytes = new FileInputStream(downloadFile).readAllBytes();
                    exchange.sendResponseHeaders(200, fileBytes.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(fileBytes);
                    os.close();
                    return;
                }

                String jsonResponse = controller.handleRequest(path, query, method, body);
                byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(responseBytes);
                os.close();
            }
        });

        // Static Web Files Handler
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String path = exchange.getRequestURI().getPath();
                if (path.equals("/")) path = "/index.html";

                String resourcePath = "web" + path;
                InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
                
                if (is == null) {
                    // Try direct file path
                    File f = new File("src/main/resources/web" + path);
                    if (f.exists()) {
                        is = new FileInputStream(f);
                    }
                }

                if (is == null) {
                    String notFound = "404 Not Found";
                    exchange.sendResponseHeaders(404, notFound.length());
                    exchange.getResponseBody().write(notFound.getBytes());
                    exchange.getResponseBody().close();
                    return;
                }

                String contentType = "text/html";
                if (path.endsWith(".css")) contentType = "text/css";
                else if (path.endsWith(".js")) contentType = "application/javascript";
                else if (path.endsWith(".png")) contentType = "image/png";
                else if (path.endsWith(".svg")) contentType = "image/svg+xml";

                exchange.getResponseHeaders().set("Content-Type", contentType);
                byte[] bytes = is.readAllBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();
                is.close();
            }
        });

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("=========================================================");
        System.out.println(" BusGuard AI Recovery Workbench Server Running!");
        System.out.println(" Access Web Workbench at: http://localhost:" + this.port);
        System.out.println("=========================================================");
        return this.port;
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    public int getPort() { return port; }

    private String getQueryParam(String query, String key) {
        if (query == null || query.isEmpty()) return "";
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2 && kv[0].equals(key)) return kv[1];
        }
        return "";
    }
}
