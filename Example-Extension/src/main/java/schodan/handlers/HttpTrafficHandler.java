package schodan.handlers;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Annotations;
import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.http.handler.HttpHandler;
import burp.api.montoya.http.handler.HttpRequestToBeSent;
import burp.api.montoya.http.handler.RequestToBeSentAction;
import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.http.handler.ResponseReceivedAction;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import schodan.ui.MainUITab;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Intercepts HTTP requests and responses from any tool (Proxy, Repeater, Scanner, Intruder, etc.).
 **/
public class HttpTrafficHandler implements HttpHandler {
    private final MontoyaApi montoyaApi;
    private MainUITab mainUITab;

    public HttpTrafficHandler(MontoyaApi montoyaApi) {
        this.montoyaApi = montoyaApi;
    }
    
    public void setMainUITab(MainUITab mainUITab) {
        this.mainUITab = mainUITab;
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        try {
            // requestToBeSent is already the HttpRequest object, so we can modify it directly

            // ----- Annotations -----
            // define annotations - they can be used to highlight the request/response in the Burp UI
            Annotations ann = requestToBeSent.annotations();

            // ----- Inspect Headers -----
            if ("TRACE".equalsIgnoreCase(requestToBeSent.method())) {
                ann = ann.withNotes("Request was a TRACE");
            }

            // ----- Add Headers -----
            HttpRequest updatedRequest = requestToBeSent.withAddedHeader("X-Template-Header", "TemplateExtension");
            
            // Log to UI tab if URL matches filter
            if (mainUITab != null) {
                String url = requestToBeSent.url().toString();
                if (mainUITab.matchesUrlFilter(url)) {
                    mainUITab.appendLog("[HTTP LISTENER] A request was sent/intercepted: " + url);
                }
            }
            
            return RequestToBeSentAction.continueWith(updatedRequest, ann);

            
        } catch (Exception e) {
            montoyaApi.logging().logToError("Error logging HTTP request: " + e.getMessage());
        }
        return RequestToBeSentAction.continueWith(requestToBeSent);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        Annotations ann = responseReceived.annotations();
        try {
            // responseReceived is already the HttpResponse object, so we can modify it directly
            HttpRequest initiating = responseReceived.initiatingRequest();

            // ----- Annotations -----
            // define annotations - they can be used to highlight the request/response in the Burp UI

            // ----- Inspect Headers -----
            //look for specific headers
            String serverHeader = responseReceived.headerValue("Server");
            if (serverHeader != null && serverHeader.contains("Apache")) {
                ann = ann.withHighlightColor(HighlightColor.YELLOW);
            }

            if (!responseReceived.hasHeader("Content-Security-Policy")) {
                ann = ann.withNotes("Custom note: No CSP");
            }

            if (responseReceived.bodyToString().contains("password=")) {
                String note = ann.hasNotes() ? ann.notes() + "\n" : "";
                ann = ann.withNotes(note + "Possible sensitive info in response body.");
                ann = ann.withHighlightColor(HighlightColor.ORANGE);
            }

        } catch (Exception e) {
            montoyaApi.logging().logToError("Error logging HTTP response: " + e.getMessage());
            return ResponseReceivedAction.continueWith(responseReceived);
        }
        return ResponseReceivedAction.continueWith(responseReceived, ann);
    }
}
