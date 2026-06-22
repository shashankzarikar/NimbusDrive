package com.nimbusdrive.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    private static final String BREVO_ENDPOINT = "https://api.brevo.com/v3/smtp/email";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final String brevoApiKey;
    private final String senderEmail;

    public EmailService(@Value("${NIMBUSDRIVE_BREVO_API_KEY}") String brevoApiKey,
                        @Value("${NIMBUSDRIVE_MAIL_USERNAME}") String senderEmail) {
        this.brevoApiKey = brevoApiKey;
        this.senderEmail = senderEmail;
    }

    public void sendEmail(String to, String subject, String body) {
        String jsonPayload = buildJsonPayload(to, subject, body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(BREVO_ENDPOINT))
                .header("api-key", brevoApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new RuntimeException("Failed to send email via Brevo: " + response.body());
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException("Failed to send email via Brevo", e);
        }
    }

    private String buildJsonPayload(String to, String subject, String body) {
        return "{" +
                "\"sender\": { \"email\": \"" + escapeJson(senderEmail) + "\" }, " +
                "\"to\": [{ \"email\": \"" + escapeJson(to) + "\" }], " +
                "\"subject\": \"" + escapeJson(subject) + "\", " +
                "\"textContent\": \"" + escapeJson(body) + "\"" +
                "}";
    }

    private String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
