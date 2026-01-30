package schodan.handlers;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Annotations;
import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.proxy.http.InterceptedRequest;
import burp.api.montoya.proxy.http.InterceptedResponse;
import burp.api.montoya.proxy.http.ProxyRequestHandler;
import burp.api.montoya.proxy.http.ProxyRequestReceivedAction;
import burp.api.montoya.proxy.http.ProxyRequestToBeSentAction;
import burp.api.montoya.proxy.http.ProxyResponseHandler;
import burp.api.montoya.proxy.http.ProxyResponseReceivedAction;
import burp.api.montoya.proxy.http.ProxyResponseToBeSentAction;

/**
 * Intercepts requests and responses from the Proxy.
 * 
 * Browser → (initial intercept) → Proxy → (final intercept) → Server
 * Server → (initial intercept) → Proxy → (final intercept) → Browser
 */
public class ProxyInterceptionHandler implements ProxyRequestHandler, ProxyResponseHandler {
    private final MontoyaApi montoyaApi;

    public ProxyInterceptionHandler(MontoyaApi montoyaApi) {
        this.montoyaApi = montoyaApi;
    }

    /**
     * Proxy receives a browser request before any modification/forwarding rules
     */
    @Override
    public ProxyRequestReceivedAction handleRequestReceived(InterceptedRequest interceptedRequest) {
        try {
            Annotations ann = interceptedRequest.annotations();

            if (interceptedRequest.hasHeader("X-Debug")) {
                ann = ann.withNotes("Client sent X-Debug");
                ann = ann.withHighlightColor(HighlightColor.YELLOW);
            }
            return ProxyRequestReceivedAction.continueWith(interceptedRequest, ann);

        } catch (Exception e) {
            montoyaApi.logging().logToError("Error handling proxy request: " + e.getMessage());
            return ProxyRequestReceivedAction.continueWith(interceptedRequest);
        }
    }

    /**
     * Right before the Proxy forwards the request to the server
     */
    @Override
    public ProxyRequestToBeSentAction handleRequestToBeSent(InterceptedRequest interceptedRequest) {
        HttpRequest updatedRequest = interceptedRequest.withAddedHeader("X-Proxy-Injected", "true");
        return ProxyRequestToBeSentAction.continueWith(updatedRequest);
    }

    /**
     * When the Proxy receives a server response before it applies rewriting/filtering
     */
    @Override
    public ProxyResponseReceivedAction handleResponseReceived(InterceptedResponse interceptedResponse) {
        Annotations ann = interceptedResponse.annotations();

        if (!interceptedResponse.hasHeader("Content-Security-Policy")) {
            ann = ann.withNotes("Missing CSP header");
            ann = ann.withHighlightColor(HighlightColor.ORANGE);
        }

        return ProxyResponseReceivedAction.continueWith(interceptedResponse, ann);
    }

    /**
     * Right before the Proxy returns the response to the browser
     */
    @Override
    public ProxyResponseToBeSentAction handleResponseToBeSent(InterceptedResponse interceptedResponse) {
        return ProxyResponseToBeSentAction.continueWith(interceptedResponse);
    }
}
