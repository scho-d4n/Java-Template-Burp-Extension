# Burp Extension Template

Minimal skeleton for Burp Suite extensions (Montoya API). Includes common handlers and a light UI with example components.

[TOC]

## Structure

The template has the main `BurpExtension` class as well as several common handlers, such as `HttpHandler`, `ProxyRequestHandler`, `ProxyResponseHandler`, `ProxyMessageHandler` (WebSockets) and `ContextMenuItemsProvider`.



### Main Class

*TemplateExtension.java*

```java
public class TemplateExtension implements BurpExtension, ExtensionUnloadingHandler {
    private static final String EXTENSION_NAME = "Template Extension";
    
    //[...]
    
    @Override
    public void initialize(MontoyaApi montoyaApi) {
        try {
            montoyaApi.extension().setName(EXTENSION_NAME);
            
            //[register handlers]
            
            montoyaApi.extension().registerUnloadingHandler(this);
            
       	} catch (Exception e) {
            montoyaApi.logging().logToError("Failed to initialize extension: " + e.getMessage());
            throw e;
        }
    }
    
    @Override
    public void extensionUnloaded() {
        unload();
    }
    
    //[...]
}
```



### Handlers

#### HttpHandler

Intercepts HTTP requests and responses from any tool (Proxy, Repeater, Scanner, Intruder, etc.).

```java
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
        // Log to UI tab if URL matches filter
        if (mainUITab != null) {
            String url = requestToBeSent.url().toString();
            if (mainUITab.matchesUrlFilter(url)) {
                mainUITab.appendLog("[HTTP LISTENER] A request was sent/intercepted: " + url);
            }
        }
        //handle request
    }
    
    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        //handle response
    }
}
```

**Features:**
- Logs requests to the UI tab when they match the URL filter
- Filter is set via the URL text field in the UI tab



#### ProxyRequestHandler and ProxyResponseHandler

Intercepts requests and responses from the Proxy only. However, there are additional stages at which requests and responses can be intercepted.

- Browser → (initial intercept) → Proxy → (final intercept) → Server
- Server → (initial intercept) → Proxy → (final intercept) → Browser

```java
public class ProxyInterceptionHandler implements ProxyRequestHandler, ProxyResponseHandler {
    private final MontoyaApi montoyaApi;
    private MainUITab mainUITab;

    public ProxyInterceptionHandler(MontoyaApi montoyaApi) {
        this.montoyaApi = montoyaApi;
    }
    
    public void setMainUITab(MainUITab mainUITab) {
        this.mainUITab = mainUITab;
    }
    
    @Override
    public ProxyRequestReceivedAction handleRequestReceived(InterceptedRequest interceptedRequest) {
        // Log to UI tab if URL matches filter
        if (mainUITab != null) {
            String url = interceptedRequest.url().toString();
            if (mainUITab.matchesUrlFilter(url)) {
                mainUITab.appendLog("[PROXY LISTENER] A request was intercepted: " + url);
            }
        }
        //Proxy receives a browser request before any modification/forwarding rules
    }
    
    @Override
    public ProxyRequestToBeSentAction handleRequestToBeSent(InterceptedRequest interceptedRequest) {
        //Right before the Proxy forwards the request to the server
    }
    
    @Override
    public ProxyResponseReceivedAction handleResponseReceived(InterceptedResponse interceptedResponse) {
        //When the Proxy receives a server response before it applies rewriting/filtering
    }
    
    @Override
    public ProxyResponseToBeSentAction handleResponseToBeSent(InterceptedResponse interceptedResponse) {
        //Right before the Proxy returns the response to the browser
    }
}
```

**Features:**
- Logs intercepted requests to the UI tab when they match the URL filter
- Filter is set via the URL text field in the UI tab



#### ProxyMessageHandler (WebSockets)

Intercepts WebSocket messages from the Proxy.

```java
public class WebSocketHandler implements ProxyMessageHandler {
    private final MontoyaApi montoyaApi;

    public WebSocketHandler(MontoyaApi montoyaApi) {
        this.montoyaApi = montoyaApi;
    }
    
    @Override
    public TextMessageReceivedAction handleTextMessageReceived(InterceptedTextMessage textMessage) {
        //Proxy receives a text message from the server
    }
    
    @Override
    public TextMessageToBeSentAction handleTextMessageToBeSent(InterceptedTextMessage textMessage) {
        //Proxy sends a text message to the server
    }
    
    @Override
    public BinaryMessageReceivedAction handleBinaryMessageReceived(InterceptedBinaryMessage binaryMessage) {
        //Proxy receives a binary message from the server
    }
    
    @Override
    public BinaryMessageToBeSentAction handleBinaryMessageToBeSent(InterceptedBinaryMessage binaryMessage) {
        //Proxy sends a binary message to the server
    }
}
```



#### ContextMenuItemsProvider

Offer context menu options in the Proxy, Request/Response windows etc.

```java
public class ContextMenuHandler implements ContextMenuItemsProvider {
    private final MontoyaApi montoyaApi;
    private MainUITab mainUITab;
    
    public ContextMenuHandler(MontoyaApi montoyaApi) {
        this.montoyaApi = montoyaApi;
    }
    
    public void setMainUITab(MainUITab mainUITab) {
        this.mainUITab = mainUITab;
    }
    
    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<Component> items = new ArrayList<>();

        try {
            JMenu extensionMenu = new JMenu("Template Extension");

            JMenuItem testContextMenuSourceItem = new JMenuItem("Test Context Menu Source");
            testContextMenuSourceItem.addActionListener(l -> onMenuAction(event));
            extensionMenu.add(testContextMenuSourceItem);
            
            extensionMenu.addSeparator();
            JMenuItem sendToExtensionItem = new JMenuItem("Send to Extension");
            sendToExtensionItem.addActionListener(l -> sendToExtension(event));
            extensionMenu.add(sendToExtensionItem);

            items.add(extensionMenu);
        } catch (Exception e) {
            montoyaApi.logging().logToError("Error creating context menu items: " + e.getMessage());
        }

        return items;
    }
    
    private void sendToExtension(ContextMenuEvent event) {
        HttpRequestResponse rr = getSelectedRequestResponse(event);
        if (rr != null && mainUITab != null) {
            mainUITab.setRequestResponse(rr);
            mainUITab.appendLog("[CONTEXT MENU] Request sent to extension: ...");
        }
    }
    
    private void onMenuAction(ContextMenuEvent event) {
        try {
            switch (event.invocationType()) {
                case MESSAGE_EDITOR_REQUEST:
                    handleMessageEditorRequest(event);
                    break;
                case MESSAGE_EDITOR_RESPONSE:
                    handleMessageEditorResponse(event);
                    break;
                case MESSAGE_VIEWER_REQUEST:
                    handleMessageViewerRequest(event);
                    break;
                case MESSAGE_VIEWER_RESPONSE:
                    handleMessageViewerResponse(event);
                    break;
                case PROXY_HISTORY:
                    handleProxyHistory(event);
                    break;
                case SITE_MAP_TREE:
                    handleSiteMapTree(event);
                    break;
                case SITE_MAP_TABLE:
                    handleSiteMapTable(event);
                    break;
                default:
                    handleDefault(event);
                    break;
            }
        } catch (Exception e) {
            montoyaApi.logging().logToError("Error handling context menu action: " + e.getMessage());
        }
    }
    
    //[...]
}
```

**Features:**
- **Send to Extension**: Context menu item that sends the selected request/response to the extension UI tab
- The request can then be re-sent using HTTP/1.1 or HTTP/2 buttons in the UI



### Scanner

#### Active Scan Check

Basic active scan check:

```java
public class ActiveScanCheckExample implements ActiveScanCheck {
    private final MontoyaApi montoyaApi;
    private MainUITab mainUITab;

    public ActiveScanCheckExample(MontoyaApi montoyaApi) {
        this.montoyaApi = montoyaApi;
    }
    
    public void setMainUITab(MainUITab mainUITab) {
        this.mainUITab = mainUITab;
    }

    @Override
    public String checkName() {
        return "Missing X-Frame-Options";
    }
    
    @Override
    public AuditResult doCheck(HttpRequestResponse baseRequestResponse, AuditInsertionPoint insertionPoint, Http http) {
        try {
            // Check for missing X-Frame-Options header
            if (issueFound) {
                if (mainUITab != null) {
                    mainUITab.appendLog("[ACTIVE SCAN] Missing X-Frame-Options header found: ...");
                }
                return AuditResult.auditResult(issue);
            }
            return AuditResult.auditResult();
        } catch (Exception e) {
            montoyaApi.logging().logToError("Error in active scan check: " + e.getMessage());
            return AuditResult.auditResult();
        }
}
```

**Features:**
- Logs scan results to the UI tab when issues are found



#### Passive Scan Check

Basic passive scan check:

```java
public class PassiveScanCheckExample implements PassiveScanCheck {
    private final MontoyaApi montoyaApi;
    private MainUITab mainUITab;

    public PassiveScanCheckExample(MontoyaApi montoyaApi) {
        this.montoyaApi = montoyaApi;
    }
    
    public void setMainUITab(MainUITab mainUITab) {
        this.mainUITab = mainUITab;
    }

    @Override
    public String checkName() {
        return "Server header disclosure";
    }
    
    @Override
    public AuditResult doCheck(HttpRequestResponse baseRequestResponse) {
        try {
            // Check for Server header disclosure
            if (issueFound) {
                if (mainUITab != null) {
                    mainUITab.appendLog("[PASSIVE SCAN] Server header disclosure found: ...");
                }
                return AuditResult.auditResult(issue);
            }
            return AuditResult.auditResult();
        } catch (Exception e) {
            montoyaApi.logging().logToError("Error in passive scan check: " + e.getMessage());
            return AuditResult.auditResult();
        }
}
```

**Features:**
- Logs scan results to the UI tab when issues are found



### UI

The UI tab includes request/response editors, logging area, and various buttons:

```java
public class MainUITab extends JPanel {
    private final MontoyaApi montoyaApi;
    private final RequestSenderExample requestSender;
    
    private JTextField urlTextField;
    private JTextArea logTextArea;
    private HttpRequestEditor requestEditor;
    private HttpResponseEditor responseEditor;
    private HttpRequestResponse pendingRequestResponse;
    
    public MainUITab(MontoyaApi montoyaApi) {
        super(new BorderLayout());
        this.montoyaApi = montoyaApi;
        this.requestSender = new RequestSenderExample(montoyaApi);
        setBorder(new EmptyBorder(12, 12, 12, 12));
        try {
            initializeUI();
            loadPersistedData();
        } catch (Exception e) {
            montoyaApi.logging().logToError("Error initializing UI tab: " + e.getMessage());
        }
    }
    
    private void initializeUI() {
        // URL filter field, HTTP/1.1, HTTP/2 buttons, file operations, etc.
        // Request/response editors
        // Logging area with clear and save to project buttons
    }
}
```

**Features:**
- **Request/Response Editors**: Built-in Burp editors for viewing and editing HTTP messages
- **HTTP/1.1 Button**: Sends requests using HTTP/1.1 protocol (auto mode)
- **HTTP/1.1 Override Button**: Explicitly forces HTTP/1.1 protocol
- **HTTP/2 Button**: Sends requests using HTTP/2 protocol
- **URL Filter**: Filters proxy and HTTP listener logs based on URL
- **Logging**: Displays logs from proxy listener, HTTP listener, and scan checks
- **Context Menu Integration**: "Send to Extension" menu item sends selected request/response to the UI tab
- **Project Persistence**: Save and load extension data with the Burp project



### Utils

The `utils` package includes examples on how to create and send HTTP/1.1 and HTTP/2 requests.

```java
public class RequestSenderExample {
    private final MontoyaApi montoyaApi;
    
    public HttpRequest createHttp11Request(HttpService service, String path) {
        String request = "GET " + path + " HTTP/1.1\r\n"
                + "Host: " + service.host() + "\r\n"
                + "\r\n";
        return HttpRequest.httpRequest(service, request);
    }

    public HttpRequest createHttp2Request(HttpService service, String path) {
        List<HttpHeader> headers = List.of(
                HttpHeader.httpHeader(":method", "GET"),
                HttpHeader.httpHeader(":path", path),
                HttpHeader.httpHeader(":scheme", service.secure() ? "https" : "http"),
                HttpHeader.httpHeader("host", service.host())
        );
        return HttpRequest.http2Request(service, headers, ByteArray.byteArray(""));
    }
    
    public HttpRequest convertToHttp11(HttpRequest request) {
        // Converts HTTP/2 requests to HTTP/1.1 format
        // Removes pseudo-headers, ensures Host header
    }
    
    public void sendRequestInBackground(HttpRequest request, HttpMode httpMode, Runnable onComplete, Consumer<HttpRequestResponse> responseCallback) {
        // Sends request using SwingWorker to avoid blocking UI
    }
    
    public void sendHttp11RequestInBackground(HttpRequest request, Runnable onComplete, Consumer<HttpRequestResponse> responseCallback) {
        // Convenience method for sending HTTP/1.1 requests
    }
    
    public void sendHttp2RequestInBackground(HttpRequest request, Runnable onComplete, Consumer<HttpRequestResponse> responseCallback) {
        // Convenience method for sending HTTP/2 requests
    }
}
```

**Features:**
- Create HTTP/1.1 and HTTP/2 requests
- Convert HTTP/2 requests to HTTP/1.1 format
- Send requests in background threads using `SwingWorker` to avoid blocking UI
- Response callbacks to update UI with received responses

When sending requests or accessing shared resources, ensure you run them in a new thread and use resource locks where necessary.



## Unload and Cleanup

On unload, the extension deregisters all Burp Registrations (HTTP handler, proxy request/response, WebSocket creation, scan checks, audit issue handler, UI tab, context menu). No threads, timers, `SwingWorker`, or `ExecutorService` are created by this skeleton, so there is nothing extra to terminate. If you add background threads or executors, shut them down in `TemplateExtension.unload()` before deregistering (see PortSwigger guidelines: release all resources and terminate any extension-created threads on unload).



## Patterns

- **Context menu from anywhere:** One menu item "Test Context Menu Source" calls `onMenuAction(event)`; switch on `event.invocationType()` routes to per-context handlers.
- **Edit proxy/repeater request:** In `ProxyInterceptionHandler.handleRequestReceived`, modify the request and return `ProxyRequestReceivedAction.continueWith(modifiedRequest)`.
- **Send request off EDT:** Use `SwingWorker`; call `montoyaApi.http().sendRequest(...)` in `doInBackground()`, update UI in `done()`. Never block the EDT with HTTP.



## Additional Burp Features (reference)

| Feature | API | Notes |
|--------|-----|-------|
| **HTTP/2** | `HttpRequest.http2Request(service, headers, body)`; `montoyaApi.http().sendRequest(request, HttpMode.HTTP_2)` | Create/send HTTP/2 requests. |
| **Scope** | `montoyaApi.scope()` | `isInScope(url)`, include/exclude, ScopeChangeHandler. |
| **Persistence** | `montoyaApi.persistence().extensionData()` | Save/load extension state across restarts. Project-specific data via `extensionData()`, user-specific via `preferences()`. |
| **Parallel send** | `montoyaApi.http().sendRequests(List<HttpRequest>)` | Bulk requests; run off EDT. |
| **Session handling** | `montoyaApi.http().registerSessionHandlingAction(SessionHandlingAction)` | Custom session rule actions. |
| **Cookie jar** | `montoyaApi.http().cookieJar()` | Read/update cookies. |
| **Decoder** | `montoyaApi.decoder()` | Encode/decode (e.g. Base64, URL). |
| **Comparer** | `montoyaApi.comparer()` | Send data to Comparer (e.g. diff). |
| **Intruder** | `montoyaApi.intruder()` | Send request to Intruder (similar to Repeater). |



## Project Persistence

The template includes an example of saving and loading data with the Burp project file:

- **Save to Project**: `handleSaveProject()` saves the URL text field value using `montoyaApi.persistence().extensionData().setString()`
- **Load on Startup**: `loadPersistedData()` loads saved data when the extension initialises using `montoyaApi.persistence().extensionData().getString()`

Data saved with `extensionData()` is stored with the Burp project file and persists across Burp restarts when the project is loaded. Use `preferences()` for user-specific data that persists across projects.



## Build

```bash
./gradlew build
```

Load the generated JAR from `build/libs/` in Burp: Extensions → Add → Extension type: Java → Select JAR.
