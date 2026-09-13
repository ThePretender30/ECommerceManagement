package com.ecommerce.email;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class BrevoEmailService implements EmailService {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final String apiKey;
    private final String fromEmail;
    private final String fromName;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public BrevoEmailService(String apiKey, String fromEmail, String fromName, ObjectMapper objectMapper) {
        this.apiKey = apiKey.trim();
        this.fromEmail = fromEmail.trim();
        this.fromName = (fromName != null && !fromName.isBlank()) ? fromName.trim() : "Roz Bazaar";
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }

    @Override
    public SendResult send(String toEmail, String subject, String bodyText) {
        return send(toEmail, subject, bodyText, null);
    }

    @Override
    public SendResult send(String toEmail, String subject, String bodyText, String bodyHtml) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("sender", Map.of("email", fromEmail, "name", fromName));
            payload.put("to", List.of(Map.of("email", toEmail.trim())));
            payload.put("subject", subject);
            if (bodyText != null && !bodyText.isBlank()) {
                payload.put("textContent", bodyText);
            }
            if (bodyHtml != null && !bodyHtml.isBlank()) {
                payload.put("htmlContent", bodyHtml);
            } else if (bodyText != null) {
                payload.put("htmlContent", "<pre style=\"font-family: sans-serif; font-size: 14px;\">" + escapeHtml(bodyText) + "</pre>");
            }

            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BREVO_API_URL))
                    .timeout(TIMEOUT)
                    .header("api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String messageId = "BREVO-UNKNOWN";
                try {
                    JsonNode node = objectMapper.readTree(response.body());
                    if (node.has("messageId")) {
                        messageId = node.get("messageId").asText();
                    }
                } catch (Exception ignored) {}
                log.info("Sent email via Brevo HTTPS API to {} (messageId={})", toEmail, messageId);
                return SendResult.sent(messageId);
            } else {
                log.error("Failed to send email via Brevo HTTPS API to {}. HTTP {} - Body: {}",
                        toEmail, response.statusCode(), response.body());
                return SendResult.failed("Brevo API error (status " + response.statusCode() + "): " + response.body());
            }
        } catch (Exception e) {
            log.error("Exception sending email via Brevo HTTPS API to {}: {}", toEmail, e.getMessage(), e);
            return SendResult.failed(e.getMessage());
        }
    }

    @Override
    public String providerName() {
        return "Brevo HTTPS API";
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }
}
