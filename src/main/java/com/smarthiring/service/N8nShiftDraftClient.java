package com.smarthiring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthiring.dto.ShiftDraftResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class N8nShiftDraftClient implements ExternalShiftDraftClient {

    private static final Logger log = LoggerFactory.getLogger(N8nShiftDraftClient.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ShiftDraftJsonParser parser;
    private final boolean enabled;
    private final String webhookUrl;
    private final String webhookSecret;
    private final String matchSource;

    public N8nShiftDraftClient(boolean enabled, String webhookUrl, String webhookSecret, String matchSource) {
        this(
                enabled,
                webhookUrl,
                webhookSecret,
                matchSource,
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .connectTimeout(Duration.ofSeconds(3))
                        .build()
        );
    }

    N8nShiftDraftClient(
            boolean enabled,
            String webhookUrl,
            String webhookSecret,
            String matchSource,
            HttpClient httpClient
    ) {
        this.objectMapper = new ObjectMapper();
        this.parser = new ShiftDraftJsonParser(objectMapper);
        this.enabled = enabled;
        this.webhookUrl = webhookUrl;
        this.webhookSecret = webhookSecret;
        this.matchSource = normalizeMatchSource(matchSource);
        this.httpClient = httpClient;
    }

    @Override
    public Optional<ShiftDraftResponse> draftShift(String systemPrompt, String userPrompt) {
        if (!isConfigured()) {
            return Optional.empty();
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("systemPrompt", systemPrompt);
            body.put("userPrompt", userPrompt);
            body.put("requiredSource", matchSource);
            body.put("schema", List.of(
                    "title", "description", "requirements", "roleNeeded",
                    "pay", "date", "startTime", "endTime", "location", "assumptions", "source"
            ));

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(45))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));

            if (webhookSecret != null && !webhookSecret.isBlank()) {
                requestBuilder.header("X-StaffMatch-Webhook-Secret", webhookSecret);
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("n8n shift draft webhook returned HTTP {}", response.statusCode());
                return Optional.empty();
            }
            if (!hasRequiredSource(response.body())) {
                log.warn("n8n shift draft webhook did not return the required DeepSeek source");
                return Optional.empty();
            }

            Optional<ShiftDraftResponse> parsed = parser.parse(response.body(), matchSource);
            if (parsed.isEmpty()) {
                log.warn("n8n shift draft webhook returned an invalid response body");
            }
            return parsed;
        } catch (Exception exception) {
            log.warn("n8n shift draft webhook call failed: {}", exception.toString());
            return Optional.empty();
        }
    }

    private boolean isConfigured() {
        return enabled && webhookUrl != null && !webhookUrl.isBlank();
    }

    private boolean hasRequiredSource(String responseBody) {
        try {
            var root = objectMapper.readTree(responseBody);
            var node = root.isArray() && !root.isEmpty() ? root.get(0) : root;
            return "N8N_DEEPSEEK".equalsIgnoreCase(node.path("source").asText());
        } catch (Exception exception) {
            return false;
        }
    }

    private String normalizeMatchSource(String source) {
        return "N8N_DEEPSEEK";
    }
}
