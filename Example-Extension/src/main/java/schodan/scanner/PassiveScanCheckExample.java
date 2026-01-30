package schodan.scanner;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.scanner.AuditResult;
import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burp.api.montoya.scanner.audit.issues.AuditIssueConfidence;
import burp.api.montoya.scanner.audit.issues.AuditIssueSeverity;
import burp.api.montoya.scanner.scancheck.PassiveScanCheck;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;

public class PassiveScanCheckExample implements PassiveScanCheck {
    private final MontoyaApi montoyaApi;

    public PassiveScanCheckExample(MontoyaApi montoyaApi) {
        this.montoyaApi = montoyaApi;
    }

    @Override
    public String checkName() {
        return "Server header disclosure";
    }

    @Override
    public AuditResult doCheck(HttpRequestResponse baseRequestResponse) {
        try {
            HttpResponse response = baseRequestResponse.response();
            if (response == null) {
                return AuditResult.auditResult();
            }

            String serverValue = response.headers().stream()
                    .filter(h -> "Server".equalsIgnoreCase(h.name()))
                    .findFirst()
                    .map(h -> h.value())
                    .orElse(null);

            if (serverValue != null && !serverValue.isEmpty()) {
                AuditIssue issue = AuditIssue.auditIssue(
                        "Server header disclosure (example)",
                        "The response includes a Server header that may reveal information about the server software.",
                        baseRequestResponse.request().url().toString(),
                        baseRequestResponse.request().url().toString(),
                        AuditIssueSeverity.INFORMATION,
                        AuditIssueConfidence.CERTAIN,
                        "Server header value: " + serverValue,
                        null,
                        AuditIssueSeverity.INFORMATION,
                        baseRequestResponse
                );
                return AuditResult.auditResult(issue);
            }
            return AuditResult.auditResult();
        } catch (Exception e) {
            montoyaApi.logging().logToError("Error in passive scan check: " + e.getMessage());
            return AuditResult.auditResult();
        }
    }
}
