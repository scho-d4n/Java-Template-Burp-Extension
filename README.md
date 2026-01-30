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

    public HttpTrafficHandler(MontoyaApi montoyaApi) {
        this.montoyaApi = montoyaApi;
    }
    
    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
        //handle request
    }
    
    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
        //handle response
    }
}
```



#### ProxyRequestHandler and ProxyResponseHandler

Intercepts requests and responses from the Proxy only. However, there are additional stages at which requests and responses can be intercepted.

- Browser → (initial intercept) → Proxy → (final intercept) → Server
- Server → (initial intercept) → Proxy → (final intercept) → Browser

```java
public class ProxyInterceptionHandler implements ProxyRequestHandler, ProxyResponseHandler {
    private final MontoyaApi montoyaApi;

    public ProxyInterceptionHandler(MontoyaApi montoyaApi) {
        this.montoyaApi = montoyaApi;
    }
    
    @Override
    public ProxyRequestReceivedAction handleRequestReceived(InterceptedRequest interceptedRequest) {
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
    
    public ContextMenuHandler(MontoyaApi montoyaApi) {
        this.montoyaApi = montoyaApi;
    }
    
    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<Component> items = new ArrayList<>();

        try {
            JMenu extensionMenu = new JMenu("Template Extension");

            JMenuItem testContextMenuSourceItem = new JMenuItem("Test Context Menu Source");
            testContextMenuSourceItem.addActionListener(l -> onMenuAction(event));
            extensionMenu.add(testContextMenuSourceItem);

            items.add(extensionMenu);
        } catch (Exception e) {
            montoyaApi.logging().logToError("Error creating context menu items: " + e.getMessage());
        }

        return items;
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



### Scanner

#### Active Scan Check

Basic active scan check:

```java
public class ActiveScanCheckExample implements ActiveScanCheck {
    private final MontoyaApi montoyaApi;

    public ActiveScanCheckExample(MontoyaApi montoyaApi) {
        this.montoyaApi = montoyaApi;
    }

    @Override
    public String checkName() {
        return "Your Scan Check Name";
    }
    
    @Override
    public AuditResult doCheck(HttpRequestResponse baseRequestResponse, AuditInsertionPoint insertionPoint, Http http) {
        try {
            // add your check logic here
            return AuditResult.auditResult(issue);
        } catch (Exception e) {
            montoyaApi.logging().logToError("Error in active scan check: " + e.getMessage());
            return AuditResult.auditResult();
        }
}
```



#### Passive Scan Check

Basic passive scan check:

```java
public class PassiveScanCheckExample implements PassiveScanCheck {
    private final MontoyaApi montoyaApi;

    public PassiveScanCheckExample(MontoyaApi montoyaApi) {
        this.montoyaApi = montoyaApi;
    }

    @Override
    public String checkName() {
        return "Your Scan Check Name";
    }
    
    @Override
    public AuditResult doCheck(HttpRequestResponse baseRequestResponse) {
        try {
            // add your check logic here
            return AuditResult.auditResult();
        } catch (Exception e) {
            montoyaApi.logging().logToError("Error in passive scan check: " + e.getMessage());
            return AuditResult.auditResult();
        }
}
```



### UI

Basic UI example:

```java
public class MainUITab extends JPanel {
    private final MontoyaApi montoyaApi;
    
    public MainUITab(MontoyaApi montoyaApi) {
        super(new BorderLayout());
        this.montoyaApi = montoyaApi;
        setBorder(new EmptyBorder(12, 12, 12, 12));
        try {
            add(createContentPanel(), BorderLayout.NORTH);
        } catch (Exception e) {
            montoyaApi.logging().logToError("Error initializing UI tab: " + e.getMessage());
        }
    }
    
    private JPanel createContentPanel() {
        JPanel main = new JPanel();
        //[...]
        return main;
    }
}
```



### Utils

The `utils` package also includes examples on how to create a new HTTP 1.1 and HTTP 2 request.

```java
//[...]
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
//[...]
```

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
| **Persistence** | `montoyaApi.persistence().extensionData()` | Save/load extension state across restarts. |
| **Parallel send** | `montoyaApi.http().sendRequests(List<HttpRequest>)` | Bulk requests; run off EDT. |
| **Session handling** | `montoyaApi.http().registerSessionHandlingAction(SessionHandlingAction)` | Custom session rule actions. |
| **Cookie jar** | `montoyaApi.http().cookieJar()` | Read/update cookies. |
| **Decoder** | `montoyaApi.decoder()` | Encode/decode (e.g. Base64, URL). |
| **Comparer** | `montoyaApi.comparer()` | Send data to Comparer (e.g. diff). |
| **Intruder** | `montoyaApi.intruder()` | Send request to Intruder (similar to Repeater). |



## Build

```bash
./gradlew build
```

Load the generated JAR from `build/libs/` in Burp: Extensions → Add → Extension type: Java → Select JAR.
