package schodan.utils;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;

public class RepeaterIntegration {
    private final MontoyaApi montoyaApi;

    public RepeaterIntegration(MontoyaApi montoyaApi) {
        this.montoyaApi = montoyaApi;
    }

    public void sendToRepeater(HttpRequest request) {
        try {
            montoyaApi.repeater().sendToRepeater(request);
            montoyaApi.logging().logToOutput("Sent request to Repeater: " + request.url());
        } catch (Exception e) {
            montoyaApi.logging().logToError("Error sending to Repeater: " + e.getMessage());
        }
    }

    public void sendToRepeater(HttpRequest request, String name) {
        try {
            montoyaApi.repeater().sendToRepeater(request, name);
            montoyaApi.logging().logToOutput("Sent request to Repeater with name '" + name + "': " + request.url());
        } catch (Exception e) {
            montoyaApi.logging().logToError("Error sending to Repeater: " + e.getMessage());
        }
    }
}

