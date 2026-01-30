package schodan;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Registration;
import burp.api.montoya.extension.ExtensionUnloadingHandler;
import schodan.handlers.ContextMenuHandler;
import schodan.handlers.HttpTrafficHandler;
import schodan.handlers.ProxyInterceptionHandler;
import schodan.handlers.SettingsPanelHandler;
import schodan.handlers.WebSocketHandler;
import schodan.scanner.ActiveScanCheckExample;
import schodan.scanner.PassiveScanCheckExample;
import schodan.scanner.ScanResultsHandler;
import schodan.ui.MainUITab;

public class TemplateExtension implements BurpExtension, ExtensionUnloadingHandler {
    private static final String EXTENSION_NAME = "Template Extension";

    private Registration uiTabReg;
    private Registration contextMenuReg;
    private Registration httpHandlerReg;
    private Registration proxyRequestHandlerReg;
    private Registration proxyResponseHandlerReg;
    private Registration webSocketCreationHandlerReg;
    private Registration activeScanCheckReg;
    private Registration passiveScanCheckReg;
    private Registration auditIssueHandlerReg;
    private Registration settingsPanelReg;

    @Override
    public void initialize(MontoyaApi montoyaApi) {
        try {
            montoyaApi.extension().setName(EXTENSION_NAME);

            // Intercept requests from any tool (Proxy, Repeater, Scanner, Intruder, etc.)
            HttpTrafficHandler httpHandler = new HttpTrafficHandler(montoyaApi);
            httpHandlerReg = montoyaApi.http().registerHttpHandler(httpHandler);

            // Intercept requests from Proxy
            ProxyInterceptionHandler proxyHandler = new ProxyInterceptionHandler(montoyaApi);
            proxyRequestHandlerReg = montoyaApi.proxy().registerRequestHandler(proxyHandler);
            proxyResponseHandlerReg = montoyaApi.proxy().registerResponseHandler(proxyHandler);

            // Intercept WebSocket connections
            webSocketCreationHandlerReg = montoyaApi.proxy().registerWebSocketCreationHandler(webSocketCreation ->
                    webSocketCreation.proxyWebSocket().registerProxyMessageHandler(new WebSocketHandler(montoyaApi)));

            // Handle scan results
            ScanResultsHandler scanResultsHandler = new ScanResultsHandler(montoyaApi);
            activeScanCheckReg = montoyaApi.scanner().registerActiveScanCheck(
                    new ActiveScanCheckExample(montoyaApi),
                    burp.api.montoya.scanner.scancheck.ScanCheckType.PER_INSERTION_POINT
            );
            passiveScanCheckReg = montoyaApi.scanner().registerPassiveScanCheck(
                    new PassiveScanCheckExample(montoyaApi),
                    burp.api.montoya.scanner.scancheck.ScanCheckType.PER_REQUEST
            );
            auditIssueHandlerReg = montoyaApi.scanner().registerAuditIssueHandler(scanResultsHandler);

            // Add a main UI tab
            MainUITab mainUITab = new MainUITab(montoyaApi);
            uiTabReg = montoyaApi.userInterface().registerSuiteTab(EXTENSION_NAME, mainUITab);

            // Add a context menu
            contextMenuReg = montoyaApi.userInterface().registerContextMenuItemsProvider(new ContextMenuHandler(montoyaApi));

            // Add a settings panel (handler holds panel + getters for use elsewhere)
            SettingsPanelHandler settingsPanelHandler = new SettingsPanelHandler(montoyaApi);
            settingsPanelReg = montoyaApi.userInterface().registerSettingsPanel(settingsPanelHandler.getPanel());

            // Register an unloading handler to clean up on extension unload
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

    /**
     * Release all resources on extension unload (PortSwigger guidelines).
     * All Burp Registrations are deregistered so handlers are no longer invoked.
     * This extension does not create any Thread, Timer, SwingWorker, or ExecutorService;
     * if you add such in the future, cancel/shutdown them here before deregistering.
     */
    public void unload() {
        try {
            if (httpHandlerReg != null) {
                httpHandlerReg.deregister();
                httpHandlerReg = null;
            }
            if (proxyRequestHandlerReg != null) {
                proxyRequestHandlerReg.deregister();
                proxyRequestHandlerReg = null;
            }
            if (proxyResponseHandlerReg != null) {
                proxyResponseHandlerReg.deregister();
                proxyResponseHandlerReg = null;
            }
            if (webSocketCreationHandlerReg != null) {
                webSocketCreationHandlerReg.deregister();
                webSocketCreationHandlerReg = null;
            }
            if (activeScanCheckReg != null) {
                activeScanCheckReg.deregister();
                activeScanCheckReg = null;
            }
            if (passiveScanCheckReg != null) {
                passiveScanCheckReg.deregister();
                passiveScanCheckReg = null;
            }
            if (auditIssueHandlerReg != null) {
                auditIssueHandlerReg.deregister();
                auditIssueHandlerReg = null;
            }
            if (uiTabReg != null) {
                uiTabReg.deregister();
                uiTabReg = null;
            }
            if (contextMenuReg != null) {
                contextMenuReg.deregister();
                contextMenuReg = null;
            }
            if (settingsPanelReg != null) {
                settingsPanelReg.deregister();
                settingsPanelReg = null;
            }
        } catch (Exception e) {
            System.err.println("Error during extension unload: " + e.getMessage());
        }
    }
}
