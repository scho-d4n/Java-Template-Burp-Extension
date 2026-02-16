package schodan.utils;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.HttpMode;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.HttpHeader;

import javax.swing.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Example utils: create HTTP/1.1 and HTTP/2 requests, and send a request off the EDT using SwingWorker.
 * Not wired to the UI; use as reference when implementing "Send" from your tab.
 */
public class RequestSenderExample {

    private final MontoyaApi montoyaApi;

    public RequestSenderExample(MontoyaApi montoyaApi) {
        this.montoyaApi = montoyaApi;
    }

    /** Build a minimal HTTP/1.1 GET request (from raw bytes; Burp parses and uses HTTP/1). */
    public HttpRequest createHttp11Request(HttpService service, String path) {
        String request = "GET " + path + " HTTP/1.1\r\n"
                + "Host: " + service.host() + "\r\n"
                + "\r\n";
        return HttpRequest.httpRequest(service, request);
    }

    /** Build a minimal HTTP/2 request using the API (sends as HTTP/2 when using HttpMode.HTTP_2). */
    public HttpRequest createHttp2Request(HttpService service, String path) {
        List<HttpHeader> headers = List.of(
                HttpHeader.httpHeader(":method", "GET"),
                HttpHeader.httpHeader(":path", path),
                HttpHeader.httpHeader(":scheme", service.secure() ? "https" : "http"),
                HttpHeader.httpHeader("host", service.host())
        );
        return HttpRequest.http2Request(service, headers, ByteArray.byteArray(""));
    }

    /**
     * Convert an existing HttpRequest to HTTP/1.1 format.
     */
    public HttpRequest convertToHttp11(HttpRequest request) {
        HttpService service = request.httpService();
        String method = request.method();
        String path = request.path();
        String query = request.query();
        String fullPath = query != null && !query.isEmpty() ? path + "?" + query : path;
        
        StringBuilder requestBuilder = new StringBuilder();
        requestBuilder.append(method).append(" ").append(fullPath).append(" HTTP/1.1\r\n");
        
        boolean hasHostHeader = false;
        
        // Add headers
        for (HttpHeader header : request.headers()) {
            String name = header.name();
            // Skip HTTP/2 pseudo-headers
            if (name.startsWith(":")) {
                // Handle :authority header (HTTP/2) - convert to Host header
                if (":authority".equals(name)) {
                    requestBuilder.append("Host: ").append(header.value()).append("\r\n");
                    hasHostHeader = true;
                }
            } else {
                if ("host".equalsIgnoreCase(name)) {
                    hasHostHeader = true;
                }
                requestBuilder.append(name).append(": ").append(header.value()).append("\r\n");
            }
        }
        
        // Ensure Host header is present
        if (!hasHostHeader) {
            requestBuilder.append("Host: ").append(service.host());
            if (service.port() != (service.secure() ? 443 : 80)) {
                requestBuilder.append(":").append(service.port());
            }
            requestBuilder.append("\r\n");
        }
        
        requestBuilder.append("\r\n");
        
        // Add body if present
        ByteArray body = request.body();
        if (body != null && body.length() > 0) {
            requestBuilder.append(body.toString());
        }
        
        return HttpRequest.httpRequest(service, requestBuilder.toString());
    }

    /**
     * Check if a request is already in HTTP/2 format.
     */
    public boolean isHttp2Request(HttpRequest request) {
        // Check if request has HTTP/2 pseudo-headers
        for (HttpHeader header : request.headers()) {
            String name = header.name();
            if (name.startsWith(":") && (":method".equals(name) || ":path".equals(name) || ":scheme".equals(name) || ":authority".equals(name))) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Convert an existing HttpRequest to HTTP/2 format.
     * If the request is already HTTP/2, it returns the original request unchanged to avoid kettling.
     */
    public HttpRequest convertToHttp2(HttpRequest request) {
        // If already HTTP/2, return as-is without any modification to avoid kettling
        // Even if HTTPS is needed, we preserve the original request structure
        if (isHttp2Request(request)) {
            // Return the original request unchanged - any service changes should be handled by Burp
            return request;
        }
        
        HttpService service = request.httpService();
        
        // HTTP/2 typically requires HTTPS - ensure service is secure
        if (!service.secure()) {
            service = HttpService.httpService(service.host(), service.port(), true);
        }
        
        String method = request.method();
        String path = request.path();
        String query = request.query();
        String fullPath = query != null && !query.isEmpty() ? path + "?" + query : path;
        
        java.util.ArrayList<HttpHeader> headers = new java.util.ArrayList<>();
        
        // Add HTTP/2 pseudo-headers (must be first and in specific order)
        headers.add(HttpHeader.httpHeader(":method", method));
        headers.add(HttpHeader.httpHeader(":path", fullPath));
        headers.add(HttpHeader.httpHeader(":scheme", "https"));
        
        // Build :authority header with port if not default
        String authority = service.host();
        int port = service.port();
        if (port != 443) {
            authority = authority + ":" + port;
        }
        headers.add(HttpHeader.httpHeader(":authority", authority));
        
        // Add regular headers (skip HTTP/2 pseudo-headers and Host header if present)
        // HTTP/2 requires all header names to be lowercase
        for (HttpHeader header : request.headers()) {
            String name = header.name();
            // Skip HTTP/2 pseudo-headers and Host header (replaced by :authority)
            if (!name.startsWith(":") && !"host".equalsIgnoreCase(name)) {
                // Ensure header name is lowercase for HTTP/2 compliance
                String lowerName = name.toLowerCase();
                headers.add(HttpHeader.httpHeader(lowerName, header.value()));
            }
        }
        
        ByteArray body = request.body();
        if (body == null) {
            body = ByteArray.byteArray("");
        }
        
        return HttpRequest.http2Request(service, headers, body);
    }

    /**
     * Send a request in a background thread (never block the EDT).
     * Call this from a button listener; onComplete runs on the EDT.
     */
    public void sendRequestInBackground(HttpRequest request, Runnable onComplete) {
        sendRequestInBackground(request, onComplete, null);
    }
    
    /**
     * Send a request in a background thread with response callback.
     */
    public void sendRequestInBackground(HttpRequest request, Runnable onComplete, Consumer<HttpRequestResponse> responseCallback) {
        sendRequestInBackground(request, HttpMode.AUTO, onComplete, responseCallback);
    }
    
    /**
     * Send a request in a background thread with explicit HttpMode and response callback.
     */
    public void sendRequestInBackground(HttpRequest request, HttpMode httpMode, Runnable onComplete, Consumer<HttpRequestResponse> responseCallback) {
        new SwingWorker<HttpRequestResponse, Void>() {
            @Override
            protected HttpRequestResponse doInBackground() throws Exception {
                return montoyaApi.http().sendRequest(request, httpMode);
            }

            @Override
            protected void done() {
                try {
                    HttpRequestResponse result = get();
                    if (result != null) {
                        montoyaApi.logging().logToOutput(
                                "Response: " + result.response().statusCode() + " for " + request.url());
                        if (responseCallback != null) {
                            responseCallback.accept(result);
                        }
                    }
                    if (onComplete != null) onComplete.run();
                } catch (Exception e) {
                    montoyaApi.logging().logToError("Send failed: " + e.getMessage());
                }
            }
        }.execute();
    }
    
    /**
     * Send an HTTP/1.1 request in background (forces HTTP/1 via HttpMode).
     */
    public void sendHttp11RequestInBackground(HttpRequest request, Runnable onComplete, Consumer<HttpRequestResponse> responseCallback) {
        sendRequestInBackground(request, HttpMode.HTTP_1, onComplete, responseCallback);
    }

    /**
     * Send an HTTP/2 request in background (forces HTTP/2 via HttpMode).
     */
    public void sendHttp2RequestInBackground(HttpRequest request, Runnable onComplete) {
        sendHttp2RequestInBackground(request, onComplete, null);
    }
    
    /**
     * Send an HTTP/2 request in background with response callback.
     */
    public void sendHttp2RequestInBackground(HttpRequest request, Runnable onComplete, Consumer<HttpRequestResponse> responseCallback) {
        new SwingWorker<HttpRequestResponse, Void>() {
            @Override
            protected HttpRequestResponse doInBackground() throws Exception {
                try {
                    return montoyaApi.http().sendRequest(request, HttpMode.HTTP_2);
                } catch (Exception e) {
                    montoyaApi.logging().logToError("HTTP/2 send error in background: " + e.getMessage());
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    HttpRequestResponse result = get();
                    if (result != null) {
                        montoyaApi.logging().logToOutput(
                                "HTTP/2 Response: " + result.response().statusCode() + " for " + request.url());
                        if (responseCallback != null) {
                            responseCallback.accept(result);
                        }
                    } else {
                        montoyaApi.logging().logToError("HTTP/2 request returned null response");
                        if (responseCallback != null) {
                            responseCallback.accept(null);
                        }
                    }
                    if (onComplete != null) onComplete.run();
                } catch (Exception e) {
                    montoyaApi.logging().logToError("HTTP/2 send failed: " + e.getMessage());
                    e.printStackTrace();
                    if (responseCallback != null) {
                        responseCallback.accept(null);
                    }
                    if (onComplete != null) onComplete.run();
                }
            }
        }.execute();
    }
}
