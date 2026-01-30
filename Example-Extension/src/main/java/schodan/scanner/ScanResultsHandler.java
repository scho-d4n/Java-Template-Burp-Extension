package schodan.scanner;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.scanner.audit.AuditIssueHandler;
import burp.api.montoya.scanner.audit.issues.AuditIssue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ScanResultsHandler implements AuditIssueHandler {
    private final MontoyaApi montoyaApi;
    private final List<AuditIssue> issues = new CopyOnWriteArrayList<>();

    public ScanResultsHandler(MontoyaApi montoyaApi) {
        this.montoyaApi = montoyaApi;
    }

    @Override
    public void handleNewAuditIssue(AuditIssue auditIssue) {
        try {
            issues.add(auditIssue);
        } catch (Exception e) {
            montoyaApi.logging().logToError("Error handling new audit issue: " + e.getMessage());
        }
    }

    public List<AuditIssue> getAllIssues() {
        return new ArrayList<>(issues);
    }

    public void clearIssues() {
        issues.clear();
    }
}
