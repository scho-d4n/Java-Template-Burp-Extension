package schodan.scanner;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.scanner.AuditResult;
import burp.api.montoya.scanner.audit.insertionpoint.AuditInsertionPoint;
import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burp.api.montoya.scanner.audit.issues.AuditIssueConfidence;
import burp.api.montoya.scanner.audit.issues.AuditIssueSeverity;
import burp.api.montoya.scanner.scancheck.ActiveScanCheck;
import burp.api.montoya.http.Http;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

/**
 * Basic Active Scan check for missing X-Frame-Options header.
 */
public class ActiveScanCheckExample implements ActiveScanCheck {
    private final MontoyaApi montoyaApi;

    public ActiveScanCheckExample(MontoyaApi montoyaApi) {
        this.montoyaApi = montoyaApi;
    }

    @Override
    public String checkName() {
        return "Missing X-Frame-Options (example)";
    }

    @Override
    public AuditResult doCheck(HttpRequestResponse baseRequestResponse, AuditInsertionPoint insertionPoint, Http http) {
        try {
            ByteArray payload = ByteArray.byteArray("1");
            HttpRequest request = insertionPoint.buildHttpRequestWithPayload(payload);
            HttpRequestResponse response = http.sendRequest(request);

            if (response.response() == null) {
                return AuditResult.auditResult();
            }

            HttpResponse resp = response.response();
            boolean missingHeader = resp.headers().stream()
                    .noneMatch(h -> "X-Frame-Options".equalsIgnoreCase(h.name()));

            if (missingHeader) {
                AuditIssue issue = AuditIssue.auditIssue(
                        "Missing X-Frame-Options (example)",
                        "The response did not include an X-Frame-Options header, which can help prevent clickjacking.",
                        baseRequestResponse.request().url().toString(),
                        baseRequestResponse.request().url().toString(),
                        AuditIssueSeverity.LOW,
                        AuditIssueConfidence.CERTAIN,
                        "Consider adding X-Frame-Options (e.g. DENY or SAMEORIGIN).",
                        null,
                        AuditIssueSeverity.LOW,
                        baseRequestResponse
                );
                return AuditResult.auditResult(issue);
            }
            return AuditResult.auditResult();
        } catch (Exception e) {
            montoyaApi.logging().logToError("Error in active scan check: " + e.getMessage());
            return AuditResult.auditResult();
        }
    }
}
