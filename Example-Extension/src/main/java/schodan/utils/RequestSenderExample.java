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
     * Send a request in a background thread (never block the EDT).
     * Call this from a button listener; onComplete runs on the EDT.
     */
    public void sendRequestInBackground(HttpRequest request, Runnable onComplete) {
        new SwingWorker<HttpRequestResponse, Void>() {
            @Override
            protected HttpRequestResponse doInBackground() throws Exception {
                return montoyaApi.http().sendRequest(request);
            }

            @Override
            protected void done() {
                try {
                    HttpRequestResponse result = get();
                    if (result != null) {
                        montoyaApi.logging().logToOutput(
                                "Response: " + result.response().statusCode() + " for " + request.url());
                    }
                    if (onComplete != null) onComplete.run();
                } catch (Exception e) {
                    montoyaApi.logging().logToError("Send failed: " + e.getMessage());
                }
            }
        }.execute();
    }

    /**
     * Send an HTTP/2 request in background (forces HTTP/2 via HttpMode).
     */
    public void sendHttp2RequestInBackground(HttpRequest request, Runnable onComplete) {
        new SwingWorker<HttpRequestResponse, Void>() {
            @Override
            protected HttpRequestResponse doInBackground() throws Exception {
                return montoyaApi.http().sendRequest(request, HttpMode.HTTP_2);
            }

            @Override
            protected void done() {
                try {
                    HttpRequestResponse result = get();
                    if (result != null) {
                        montoyaApi.logging().logToOutput(
                                "HTTP/2 Response: " + result.response().statusCode() + " for " + request.url());
                    }
                    if (onComplete != null) onComplete.run();
                } catch (Exception e) {
                    montoyaApi.logging().logToError("HTTP/2 send failed: " + e.getMessage());
                }
            }
        }.execute();
    }
}
