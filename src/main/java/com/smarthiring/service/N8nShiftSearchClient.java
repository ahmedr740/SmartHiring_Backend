package com.smarthiring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthiring.dto.ShiftSearchResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class N8nShiftSearchClient implements ExternalShiftSearchClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ShiftSearchJsonParser parser;
    private final boolean enabled;
    private final String webhookUrl;
    private final String webhookSecret;
    private final String matchSource;

    public N8nShiftSearchClient(boolean enabled, String webhookUrl, String webhookSecret, String matchSource) {
        this(
                enabled,
                webhookUrl,
                webhookSecret,
                matchSource,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .build()
        );
    }

    N8nShiftSearchClient(
            boolean enabled,
            String webhookUrl,
            String webhookSecret,
            String matchSource,
            HttpClient httpClient
    ) {
        this.objectMapper = new ObjectMapper();
        this.parser = new ShiftSearchJsonParser(objectMapper);
        this.enabled = enabled;
        this.webhookUrl = webhookUrl;
        this.webhookSecret = webhookSecret;
        this.matchSource = normalizeMatchSource(matchSource);
        this.httpClient = httpClient;
    }

    @Override
    public Optional<ShiftSearchResponse> searchShifts(String systemPrompt, String userPrompt, Set<Long> allowedShiftIds) {
        if (!isConfigured()) {
            return Optional.empty();
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("systemPrompt", systemPrompt);
            body.put("userPrompt", userPrompt);
            body.put("requiredSource", matchSource);
            body.put("schema", List.of("interpretation", "matches"));

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));

            if (webhookSecret != null && !webhookSecret.isBlank()) {
                requestBuilder.header("X-StaffMatch-Webhook-Secret", webhookSecret);
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }

            return parser.parse(response.body(), allowedShiftIds, matchSource);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private boolean isConfigured() {
        return enabled && webhookUrl != null && !webhookUrl.isBlank();
    }

    private String normalizeMatchSource(String source) {
        if ("N8N_DEEPSEEK".equalsIgnoreCase(source)) {
            return "N8N_DEEPSEEK";
        }
        return "N8N_OLLAMA";
    }
}
