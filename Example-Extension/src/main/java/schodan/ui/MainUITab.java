package schodan.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.persistence.PersistedObject;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;
import schodan.utils.RequestSenderExample;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Main UI tab with request/response editors, buttons, and logging area.
 */
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
        JPanel topPanel = new JPanel(new BorderLayout());
        
        // Top row with URL field and buttons
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topRow.add(new JLabel("Listener URL Filter"));
        urlTextField = new JTextField("http://scanme.org/", 15);
        urlTextField.setToolTipText("URL filter for proxy and HTTP listener logs");
        topRow.add(urlTextField);
        
        JButton http11Button = new JButton("HTTP/1.1");
        http11Button.addActionListener(e -> handleHttp11AutoButton());
        topRow.add(http11Button);
        
        JButton http11OverrideButton = new JButton("HTTP/1.1 Override");
        http11OverrideButton.addActionListener(e -> handleHttp11OverrideButton());
        topRow.add(http11OverrideButton);
        
        JButton http2Button = new JButton("HTTP/2");
        http2Button.addActionListener(e -> handleHttp2Button());
        topRow.add(http2Button);
        
        JButton openFileButton = new JButton("Open File");
        openFileButton.addActionListener(e -> handleOpenFile());
        topRow.add(openFileButton);
        
        JButton saveFileButton = new JButton("Save File");
        saveFileButton.addActionListener(e -> handleSaveFile());
        topRow.add(saveFileButton);
        
        JButton dialogButton = new JButton("Dialog");
        dialogButton.addActionListener(e -> handleDialog());
        topRow.add(dialogButton);
        
        topPanel.add(topRow, BorderLayout.NORTH);
        
        // Middle panel with request and response editors
        JPanel editorPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        
        JPanel requestPanel = new JPanel(new BorderLayout());
        requestPanel.setBorder(BorderFactory.createTitledBorder("Request"));
        requestEditor = montoyaApi.userInterface().createHttpRequestEditor();
        requestPanel.add(requestEditor.uiComponent(), BorderLayout.CENTER);
        editorPanel.add(requestPanel);
        
        JPanel responsePanel = new JPanel(new BorderLayout());
        responsePanel.setBorder(BorderFactory.createTitledBorder("Response"));
        responseEditor = montoyaApi.userInterface().createHttpResponseEditor();
        responsePanel.add(responseEditor.uiComponent(), BorderLayout.CENTER);
        editorPanel.add(responsePanel);
        
        topPanel.add(editorPanel, BorderLayout.CENTER);
        
        // Bottom panel with log area
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Logs"));
        logTextArea = new JTextArea(10, 50);
        logTextArea.setEditable(false);
        logTextArea.setLineWrap(true);
        logTextArea.setWrapStyleWord(true);
        JScrollPane logScrollPane = new JScrollPane(logTextArea);
        logPanel.add(logScrollPane, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> logTextArea.setText(""));
        buttonPanel.add(clearButton);
        
        JButton saveProjectButton = new JButton("Save to Project");
        saveProjectButton.addActionListener(e -> handleSaveProject());
        buttonPanel.add(saveProjectButton);
        
        logPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(topPanel, BorderLayout.CENTER);
        add(logPanel, BorderLayout.SOUTH);
    }
    
    private void handleHttp11AutoButton() {
        if (pendingRequestResponse == null || pendingRequestResponse.request() == null) {
            appendLog("[ERROR] No request available. Please send a request via context menu first.");
            return;
        }
        
        // Convert request to HTTP/1.1 format
        HttpRequest originalRequest = pendingRequestResponse.request();
        HttpRequest http11Request = requestSender.convertToHttp11(originalRequest);
        
        // Use AUTO mode - respects Burp's global settings (may use HTTP/2 if server supports it)
        requestSender.sendRequestInBackground(http11Request, () -> {
            SwingUtilities.invokeLater(() -> {
                appendLog("[HTTP/1.1] Request sent (respects global settings): " + http11Request.url());
            });
        }, (response) -> {
            SwingUtilities.invokeLater(() -> {
                if (response != null && response.response() != null) {
                    responseEditor.setResponse(response.response());
                    appendLog("[HTTP/1.1] Response received: " + response.response().statusCode() + " for " + http11Request.url());
                }
            });
        });
    }
    
    private void handleHttp11OverrideButton() {
        if (pendingRequestResponse == null || pendingRequestResponse.request() == null) {
            appendLog("[ERROR] No request available. Please send a request via context menu first.");
            return;
        }
        
        // Convert request to HTTP/1.1 format
        HttpRequest originalRequest = pendingRequestResponse.request();
        HttpRequest http11Request = requestSender.convertToHttp11(originalRequest);
        
        // Use sendHttp11RequestInBackground to force HTTP/1.1, bypassing Burp's auto HTTP/2 setting
        requestSender.sendHttp11RequestInBackground(http11Request, () -> {
            SwingUtilities.invokeLater(() -> {
                appendLog("[HTTP/1.1 Override] Request sent (forced HTTP/1.1): " + http11Request.url());
            });
        }, (response) -> {
            SwingUtilities.invokeLater(() -> {
                if (response != null && response.response() != null) {
                    responseEditor.setResponse(response.response());
                    appendLog("[HTTP/1.1 Override] Response received: " + response.response().statusCode() + " for " + http11Request.url());
                }
            });
        });
    }
    
    private void handleHttp2Button() {
        if (pendingRequestResponse == null || pendingRequestResponse.request() == null) {
            appendLog("[ERROR] No request available. Please send a request via context menu first.");
            return;
        }
        
        HttpRequest originalRequest = pendingRequestResponse.request();
        
        // Check current protocol version for logging
        boolean isAlreadyHttp2 = requestSender.isHttp2Request(originalRequest);
        appendLog("[HTTP/2] Original request protocol: " + (isAlreadyHttp2 ? "HTTP/2" : "HTTP/1.1"));
        
        // Send request as-is with HttpMode.HTTP_2 to force HTTP/2 protocol
        // Burp will handle the conversion if needed, preserving the original request structure
        requestSender.sendHttp2RequestInBackground(originalRequest, () -> {
            SwingUtilities.invokeLater(() -> {
                appendLog("[HTTP/2] Request sent: " + originalRequest.url());
            });
        }, (response) -> {
            SwingUtilities.invokeLater(() -> {
                if (response != null && response.response() != null) {
                    responseEditor.setResponse(response.response());
                    appendLog("[HTTP/2] Response received: " + response.response().statusCode() + " for " + originalRequest.url() + "\nProtocol: HTTP/2");
                } else {
                    appendLog("[HTTP/2] No response received (request may have failed or timed out)");
                }
            });
        });
    }
    
    private void handleOpenFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String filePath = selectedFile.getAbsolutePath();
            
            if (!filePath.endsWith(".txt")) {
                JOptionPane.showMessageDialog(this, "Please select a .txt file", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            try {
                String content = new String(Files.readAllBytes(Paths.get(filePath)));
                appendLog("[OPEN FILE] User opened a file with the following content:\n" + content + "\n");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "There was an error opening the file", "Error", JOptionPane.ERROR_MESSAGE);
                montoyaApi.logging().logToError("Exception details: " + e.getMessage());
            }
        }
    }
    
    private void handleSaveFile() {
        if (logTextArea.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nothing to save", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showSaveDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String filePath = selectedFile.getAbsolutePath();
            
            if (!filePath.endsWith(".txt")) {
                filePath += ".txt";
            }
            
            try {
                Files.write(Paths.get(filePath), logTextArea.getText().getBytes());
                appendLog("[SAVE FILE] User saved output to file.\n");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error saving file", "Error", JOptionPane.ERROR_MESSAGE);
                montoyaApi.logging().logToError("Exception details: " + e.getMessage());
            }
        }
    }
    
    private void handleDialog() {
        JPanel dialogPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        JTextField nameField = new JTextField(20);
        JTextField notesField = new JTextField(20);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        dialogPanel.add(new JLabel("Name:"), gbc);
        
        gbc.gridx = 1;
        dialogPanel.add(nameField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        dialogPanel.add(new JLabel("Notes:"), gbc);
        
        gbc.gridx = 1;
        dialogPanel.add(notesField, gbc);
        
        int choice = JOptionPane.showConfirmDialog(this, dialogPanel, "Example Dialog", JOptionPane.OK_CANCEL_OPTION);
        
        if (choice == JOptionPane.OK_OPTION) {
            String name = nameField.getText();
            String notes = notesField.getText();
            
            if (name.isEmpty() || notes.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all required fields.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            appendLog("[DIALOG] User entered data in the dialog:\nName: " + name + "\nNotes: " + notes + "\n");
        }
    }
    
    public void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logTextArea.append(message + "\n");
            logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
        });
    }
    
    public void setRequestResponse(HttpRequestResponse requestResponse) {
        this.pendingRequestResponse = requestResponse;
        if (requestResponse != null) {
            if (requestResponse.request() != null) {
                requestEditor.setRequest(requestResponse.request());
            }
            if (requestResponse.response() != null) {
                responseEditor.setResponse(requestResponse.response());
            }
        }
    }
    
    public String getUrlFilter() {
        return urlTextField.getText();
    }
    
    public boolean matchesUrlFilter(String url) {
        String filter = getUrlFilter().trim();
        if (filter.isEmpty()) {
            return true;
        }
        return url.contains(filter);
    }
    
    private void handleSaveProject() {
        String urlValue = urlTextField.getText();
        if (urlValue != null && !urlValue.trim().isEmpty()) {
            try {
                PersistedObject projectData = montoyaApi.persistence().extensionData();
                projectData.setString("saved_url", urlValue);
                appendLog("[PROJECT PERSISTENCE] Saved URL to project: " + urlValue);
            } catch (Exception e) {
                montoyaApi.logging().logToError("Error saving to project: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Error saving to project", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Nothing to save", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadPersistedData() {
        try {
            PersistedObject projectData = montoyaApi.persistence().extensionData();
            String savedUrl = projectData.getString("saved_url");
            if (savedUrl != null && !savedUrl.isEmpty()) {
                urlTextField.setText(savedUrl);
                appendLog("[PROJECT PERSISTENCE] Loaded saved URL from project: " + savedUrl);
            }
        } catch (Exception e) {
            montoyaApi.logging().logToError("Error loading persisted data: " + e.getMessage());
        }
    }
}
