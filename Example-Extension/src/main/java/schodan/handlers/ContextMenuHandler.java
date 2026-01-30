package schodan.handlers;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import schodan.utils.RepeaterIntegration;

import javax.swing.*;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Context menu handler for the extension.
 */
public class ContextMenuHandler implements ContextMenuItemsProvider {
    private final MontoyaApi montoyaApi;
    private final RepeaterIntegration repeaterIntegration;

    public ContextMenuHandler(MontoyaApi montoyaApi) {
        this.montoyaApi = montoyaApi;
        this.repeaterIntegration = new RepeaterIntegration(montoyaApi);
    }

    /**
     * Provides the menu items for the context menu.
     */
    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<Component> items = new ArrayList<>();

        try {
            JMenu extensionMenu = new JMenu("Template Extension");

            JMenuItem testContextMenuSourceItem = new JMenuItem("Test Context Menu Source");
            testContextMenuSourceItem.addActionListener(l -> onMenuAction(event));
            extensionMenu.add(testContextMenuSourceItem);

            extensionMenu.addSeparator();

            JMenuItem sendToEditorItem = new JMenuItem("Send to Editor");
            sendToEditorItem.addActionListener(l -> sendToRequestEditor(event));
            extensionMenu.add(sendToEditorItem);

            JMenuItem sendToRepeaterItem = new JMenuItem("Send to Repeater");
            sendToRepeaterItem.addActionListener(l -> sendToRepeater(event));
            extensionMenu.add(sendToRepeaterItem);

            items.add(extensionMenu);
        } catch (Exception e) {
            montoyaApi.logging().logToError("Error creating context menu items: " + e.getMessage());
        }

        return items;
    }

    /**
     * Sends the selected request/response to the request editor.
     */
    private void sendToRequestEditor(ContextMenuEvent event) {
        try {
            HttpRequestResponse rr = getSelectedRequestResponse(event);
            if (rr == null) {
                montoyaApi.logging().logToOutput("[Send to Editor] No request/response in this context.");
                return;
            }
            montoyaApi.logging().logToOutput("[Send to Editor] Selected: " + rr.request().method() + " " + rr.request().url() + " (add your editor UI to load this)");
        } catch (Exception e) {
            montoyaApi.logging().logToError("Error in Send to Editor: " + e.getMessage());
        }
    }

    /**
     * Sends the selected request/response to the repeater.
     */
    private void sendToRepeater(ContextMenuEvent event) {
        try {
            List<HttpRequestResponse> selected = event.selectedRequestResponses();
            if (selected != null && !selected.isEmpty()) {
                for (HttpRequestResponse item : selected) {
                    if (item != null && item.request() != null) {
                        repeaterIntegration.sendToRepeater(item.request());
                    }
                }
            }
        } catch (Exception e) {
            montoyaApi.logging().logToError("Error sending to Repeater: " + e.getMessage());
        }
    }

    /**
     * Gets the selected request/response from the context menu.
     */
    private HttpRequestResponse getSelectedRequestResponse(ContextMenuEvent event) {
        var selected = event.selectedRequestResponses();
        if (selected != null && !selected.isEmpty()) {
            return selected.get(0);
        }
        return event.messageEditorRequestResponse()
                .map(me -> me.requestResponse())
                .orElse(null);
    }

    /**
     * Handles the Test Context Menu Source action.
     */
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


    /**
     * Sub functions
     */

    private void handleMessageEditorRequest(ContextMenuEvent event) {
        montoyaApi.logging().logToOutput("MESSAGE EDITOR REQUEST");
        event.messageEditorRequestResponse().ifPresent(editor -> {
            try {
                var rr = editor.requestResponse();
                if (rr != null && rr.request() != null) {
                    montoyaApi.logging().logToOutput("[Editor Request] " + rr.request().method() + " " + rr.request().url());
                }
            } catch (Exception e) {
                montoyaApi.logging().logToError("Error processing editor request: " + e.getMessage());
            }
        });
    }

    private void handleMessageEditorResponse(ContextMenuEvent event) {
        montoyaApi.logging().logToOutput("MESSAGE EDITOR RESPONSE");
        event.messageEditorRequestResponse().ifPresent(editor -> {
            try {
                var rr = editor.requestResponse();
                if (rr != null && rr.response() != null) {
                    montoyaApi.logging().logToOutput("[Editor Response] Status " + rr.response().statusCode());
                }
            } catch (Exception e) {
                montoyaApi.logging().logToError("Error processing editor response: " + e.getMessage());
            }
        });
    }

    private void handleMessageViewerRequest(ContextMenuEvent event) {
        montoyaApi.logging().logToOutput("MESSAGE VIEWER REQUEST");
        event.messageEditorRequestResponse().ifPresent(editor -> {
            try {
                var rr = editor.requestResponse();
                if (rr != null && rr.request() != null) {
                    montoyaApi.logging().logToOutput("[Viewer Request] " + rr.request().method() + " " + rr.request().url());
                }
            } catch (Exception e) {
                montoyaApi.logging().logToError("Error processing viewer request: " + e.getMessage());
            }
        });
    }

    private void handleMessageViewerResponse(ContextMenuEvent event) {
        montoyaApi.logging().logToOutput("MESSAGE VIEWER RESPONSE");
        event.messageEditorRequestResponse().ifPresent(editor -> {
            try {
                var rr = editor.requestResponse();
                if (rr != null && rr.response() != null) {
                    montoyaApi.logging().logToOutput("[Viewer Response] Status " + rr.response().statusCode());
                }
            } catch (Exception e) {
                montoyaApi.logging().logToError("Error processing viewer response: " + e.getMessage());
            }
        });
    }

    private void handleProxyHistory(ContextMenuEvent event) {
        montoyaApi.logging().logToOutput("PROXY HISTORY");
        try {
            var selected = event.selectedRequestResponses();
            if (selected != null) {
                logSelectedSummary(selected);
            }
        } catch (Exception e) {
            montoyaApi.logging().logToError("Error processing proxy history: " + e.getMessage());
        }
    }

    private void handleSiteMapTree(ContextMenuEvent event) {
        montoyaApi.logging().logToOutput("SITE MAP TREE");
        try {
            var selected = event.selectedRequestResponses();
            if (selected != null) {
                logSelectedSummary(selected);
                logDistinctHosts(selected, "Tree hosts");
            }
        } catch (Exception e) {
            montoyaApi.logging().logToError("Error processing site map tree: " + e.getMessage());
        }
    }

    private void handleSiteMapTable(ContextMenuEvent event) {
        montoyaApi.logging().logToOutput("SITE MAP TABLE");
        try {
            var selected = event.selectedRequestResponses();
            if (selected != null) {
                logSelectedSummary(selected);
                logDistinctHosts(selected, "Table hosts");
            }
        } catch (Exception e) {
            montoyaApi.logging().logToError("Error processing site map table: " + e.getMessage());
        }
    }

    private void handleDefault(ContextMenuEvent event) {
        montoyaApi.logging().logToOutput("DEFAULT");
        try {
            var selected = event.selectedRequestResponses();
            if (selected != null) {
                montoyaApi.logging().logToOutput("Selected messages: " + selected.size());
            }
        } catch (Exception e) {
            montoyaApi.logging().logToError("Error processing default action: " + e.getMessage());
        }
    }

    private void logSelectedSummary(List<HttpRequestResponse> items) {
        if (items == null || items.isEmpty()) {
            montoyaApi.logging().logToOutput("Selected messages: 0");
            return;
        }
        try {
            montoyaApi.logging().logToOutput("Selected messages: " + items.size());
            var first = items.get(0);
            if (first != null && first.request() != null && first.request().httpService() != null) {
                var svc = first.request().httpService();
                montoyaApi.logging().logToOutput("First host: " + svc.host() + ":" + svc.port());
            }
        } catch (Exception e) {
            montoyaApi.logging().logToError("Error logging selected summary: " + e.getMessage());
        }
    }

    private void logDistinctHosts(List<HttpRequestResponse> selected, String header) {
        if (selected == null || selected.isEmpty()) return;
        try {
            var hosts = selected.stream()
                    .filter(item -> item != null && item.request() != null)
                    .map(HttpRequestResponse::request)
                    .map(req -> {
                        try { return req.httpService(); } catch (Exception e) { return null; }
                    })
                    .filter(svc -> svc != null)
                    .map(svc -> {
                        try { return svc.host(); } catch (Exception e) { return null; }
                    })
                    .filter(host -> host != null)
                    .distinct()
                    .sorted()
                    .toList();
            if (!hosts.isEmpty()) {
                montoyaApi.logging().logToOutput(header + " (" + hosts.size() + "): " + String.join(", ", hosts));
            }
        } catch (Exception e) {
            montoyaApi.logging().logToError("Error logging distinct hosts: " + e.getMessage());
        }
    }
}
