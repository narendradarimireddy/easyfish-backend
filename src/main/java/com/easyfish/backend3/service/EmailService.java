package com.easyfish.backend3.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String BREVO_SEND_EMAIL_PATH = "/v3/smtp/email";

    private final RestClient brevoClient;
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;

    public EmailService(
            RestClient.Builder restClientBuilder,
            @Value("${brevo.api-key:}") String apiKey,
            @Value("${brevo.sender-email:}") String senderEmail,
            @Value("${brevo.sender-name:}") String senderName) {
        this.brevoClient = restClientBuilder
                .baseUrl("https://api.brevo.com")
                .build();
        this.apiKey = apiKey;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
    }

    /**
     * Kept for compatibility with the existing service contract. The Easyfish order flow
     * currently calls sendPlainText only for the admin order notification.
     */
    public void sendOtp(String to, String otp) {
        sendPlainText(to, "EasyFish OTP Verification", "Your OTP is: " + otp);
    }

    /**
     * Sends email through Brevo's official HTTPS REST API.
     * Failures are logged and intentionally not rethrown, so email delivery can never
     * roll back or fail a successfully saved order.
     */
    public void sendPlainText(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            log.warn("Brevo email skipped because recipient is empty");
            return;
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.error("Brevo email skipped because BREVO_API_KEY is not configured");
            return;
        }
        if (senderEmail == null || senderEmail.isBlank()) {
            log.error("Brevo email skipped because BREVO_SENDER_EMAIL is not configured");
            return;
        }

        String normalizedSubject = subject == null ? "" : subject;
        String normalizedBody = body == null ? "" : body;
        String htmlBody = toProfessionalHtml(normalizedBody);

        Map<String, Object> requestBody = Map.of(
                "sender", Map.of(
                        "name", senderName == null || senderName.isBlank() ? senderEmail : senderName,
                        "email", senderEmail
                ),
                "to", List.of(Map.of("email", to.trim())),
                "subject", normalizedSubject,
                "textContent", normalizedBody,
                "htmlContent", htmlBody
        );

        log.info("Brevo request: endpoint={}, recipient={}, subject={}",
                BREVO_SEND_EMAIL_PATH, maskEmail(to.trim()), normalizedSubject);

        try {
            ResponseEntity<String> response = brevoClient.post()
                    .uri(BREVO_SEND_EMAIL_PATH)
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toEntity(String.class);

            String responseBody = response.getBody();
            log.info("Brevo response: HTTP status={}, body={}",
                    response.getStatusCode().value(),
                    responseBody == null || responseBody.isBlank() ? "<empty>" : responseBody);
            log.info("Brevo email sent successfully to {}", maskEmail(to.trim()));
        } catch (RestClientResponseException ex) {
            log.error("Brevo API failed: HTTP status={}, response={}, error={}",
                    ex.getStatusCode().value(), sanitizeResponse(ex.getResponseBodyAsString()), ex.getMessage());
        } catch (Exception ex) {
            log.error("Brevo email failed: {}", ex.getMessage(), ex);
        }
    }

    private String toProfessionalHtml(String body) {
        String escaped = escapeHtml(body).replace("\r\n", "\n").replace("\r", "\n");
        return """
                <!doctype html>
                <html>
                  <body style="margin:0;background:#f4f7f5;font-family:Arial,sans-serif;color:#1f2937;">
                    <div style="max-width:680px;margin:24px auto;padding:0 16px;">
                      <div style="background:#ffffff;border:1px solid #e5e7eb;border-radius:12px;overflow:hidden;">
                        <div style="padding:18px 24px;background:#0b7a4b;color:#ffffff;font-size:22px;font-weight:700;">
                          Easyfish
                        </div>
                        <div style="padding:24px;font-size:15px;line-height:1.65;white-space:pre-wrap;">%s</div>
                      </div>
                    </div>
                  </body>
                </html>
                """.formatted(escaped);
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***";
        return email.charAt(0) + "***" + email.substring(at);
    }

    private String sanitizeResponse(String response) {
        if (response == null || response.isBlank()) return "<empty>";
        return response.length() > 1000 ? response.substring(0, 1000) + "..." : response;
    }
}
