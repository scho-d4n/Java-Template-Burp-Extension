package schodan.handlers;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.proxy.websocket.ProxyMessageHandler;
import burp.api.montoya.proxy.websocket.InterceptedTextMessage;
import burp.api.montoya.proxy.websocket.InterceptedBinaryMessage;
import burp.api.montoya.proxy.websocket.TextMessageReceivedAction;
import burp.api.montoya.proxy.websocket.BinaryMessageReceivedAction;
import burp.api.montoya.proxy.websocket.TextMessageToBeSentAction;
import burp.api.montoya.proxy.websocket.BinaryMessageToBeSentAction;

/**
 * Intercepts WebSocket messages from the Proxy.
 */
public class WebSocketHandler implements ProxyMessageHandler {
    private final MontoyaApi montoyaApi;

    public WebSocketHandler(MontoyaApi montoyaApi) {
        this.montoyaApi = montoyaApi;
    }

    /**
     * When the Proxy receives a text message from the server
     */
    @Override
    public TextMessageReceivedAction handleTextMessageReceived(InterceptedTextMessage textMessage) {
        String payload = textMessage.payload();
        
        if (payload.contains("test")) {
            String updatedPayload = payload + " [intercepted]";
            return TextMessageReceivedAction.continueWith(updatedPayload);

        } else if (payload.contains("forbidden")) {
            //drop message
            return TextMessageReceivedAction.drop();
        }
        
        return TextMessageReceivedAction.continueWith(textMessage);
    }

    /**
     * Right before the Proxy sends a text message to the server
     */
    @Override
    public TextMessageToBeSentAction handleTextMessageToBeSent(InterceptedTextMessage textMessage) {
        return TextMessageToBeSentAction.continueWith(textMessage);
    }

    /**
     * When the Proxy receives a binary message from the server
     */
    @Override
    public BinaryMessageReceivedAction handleBinaryMessageReceived(InterceptedBinaryMessage binaryMessage) {
        return BinaryMessageReceivedAction.continueWith(binaryMessage);
    }

    /**
     * Right before the Proxy sends a binary message to the server
     */
    @Override
    public BinaryMessageToBeSentAction handleBinaryMessageToBeSent(InterceptedBinaryMessage binaryMessage) {
        return BinaryMessageToBeSentAction.continueWith(binaryMessage);
    }
}
