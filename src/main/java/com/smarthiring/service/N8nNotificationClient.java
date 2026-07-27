package com.smarthiring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthiring.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class N8nNotificationClient {

    private static final Logger log = LoggerFactory.getLogger(N8nNotificationClient.class);

    private final boolean enabled;
    private final String webhookUrl;
    private final String webhookSecret;
    private final String publicAppUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public N8nNotificationClient(
            @Value("${n8n.notification-enabled:false}") boolean enabled,
            @Value("${n8n.notification-webhook-url:http://localhost:5678/webhook/smart-hiring/notification-email}") String webhookUrl,
            @Value("${n8n.webhook-secret:}") String webhookSecret,
            @Value("${app.public-url:http://localhost:3000}") String publicAppUrl
    ) {
        this(
                enabled,
                webhookUrl,
                webhookSecret,
                publicAppUrl,
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .connectTimeout(Duration.ofSeconds(3))
                        .build(),
                new ObjectMapper().findAndRegisterModules()
        );
    }

    N8nNotificationClient(
            boolean enabled,
            String webhookUrl,
            String webhookSecret,
            String publicAppUrl,
            HttpClient httpClient,
            ObjectMapper objectMapper
    ) {
        this.enabled = enabled;
        this.webhookUrl = webhookUrl;
        this.webhookSecret = webhookSecret;
        this.publicAppUrl = publicAppUrl;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return enabled && webhookUrl != null && !webhookUrl.isBlank();
    }

    public boolean send(Notification notification) {
        if (!isEnabled() || notification == null || notification.getRecipient() == null
                || notification.getRecipient().getEmail() == null) {
            return false;
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("eventId", notification.getId());
            payload.put("type", notification.getType());
            payload.put("recipientEmail", notification.getRecipient().getEmail());
            payload.put("recipientName", notification.getRecipient().getName());
            payload.put("title", notification.getTitle());
            payload.put("message", notification.getMessage());
            payload.put("actionUrl", absoluteActionUrl(notification.getActionUrl()));
            payload.put("occurredAt", notification.getCreatedAt() == null ? null : notification.getCreatedAt().toString());

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)));

            if (webhookSecret != null && !webhookSecret.isBlank()) {
                builder.header("X-StaffMatch-Webhook-Secret", webhookSecret);
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            boolean succeeded = response.statusCode() >= 200 && response.statusCode() < 300;
            if (!succeeded) {
                log.warn("n8n notification webhook returned HTTP {}", response.statusCode());
            }
            return succeeded;
        } catch (Exception exception) {
            log.warn("n8n notification webhook call failed: {}", exception.toString());
            return false;
        }
    }

    private String absoluteActionUrl(String actionUrl) {
        if (actionUrl == null || actionUrl.isBlank()) {
            return publicAppUrl;
        }
        if (actionUrl.startsWith("http://") || actionUrl.startsWith("https://")) {
            return actionUrl;
        }
        return publicAppUrl.replaceAll("/$", "") + (actionUrl.startsWith("/") ? actionUrl : "/" + actionUrl);
    }
}
