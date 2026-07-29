package com.smarthiring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthiring.dto.MatchRecommendationResponse;
import com.smarthiring.dto.MatchType;
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

public class N8nMatchingClient implements ExternalMatchingClient {

    private static final Logger log = LoggerFactory.getLogger(N8nMatchingClient.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final MatchRecommendationJsonParser parser;
    private final boolean matchingEnabled;
    private final String workerMatchWebhookUrl;
    private final String managerMatchWebhookUrl;
    private final String webhookSecret;
    private final String matchSource;

    public N8nMatchingClient(
            boolean matchingEnabled,
            String workerMatchWebhookUrl,
            String managerMatchWebhookUrl,
            String webhookSecret
    ) {
        this(matchingEnabled, workerMatchWebhookUrl, managerMatchWebhookUrl, webhookSecret, "N8N_DEEPSEEK");
    }

    public N8nMatchingClient(
            boolean matchingEnabled,
            String workerMatchWebhookUrl,
            String managerMatchWebhookUrl,
            String webhookSecret,
            String matchSource
    ) {
        this(
                matchingEnabled,
                workerMatchWebhookUrl,
                managerMatchWebhookUrl,
                webhookSecret,
                matchSource,
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .connectTimeout(Duration.ofSeconds(3))
                        .build()
        );
    }

    N8nMatchingClient(
            boolean matchingEnabled,
            String workerMatchWebhookUrl,
            String managerMatchWebhookUrl,
            String webhookSecret,
            HttpClient httpClient
    ) {
        this(matchingEnabled, workerMatchWebhookUrl, managerMatchWebhookUrl, webhookSecret, "N8N_DEEPSEEK", httpClient);
    }

    N8nMatchingClient(
            boolean matchingEnabled,
            String workerMatchWebhookUrl,
            String managerMatchWebhookUrl,
            String webhookSecret,
            String matchSource,
            HttpClient httpClient
    ) {
        this.objectMapper = new ObjectMapper();
        this.parser = new MatchRecommendationJsonParser(objectMapper);
        this.matchingEnabled = matchingEnabled;
        this.workerMatchWebhookUrl = workerMatchWebhookUrl;
        this.managerMatchWebhookUrl = managerMatchWebhookUrl;
        this.webhookSecret = webhookSecret;
        this.matchSource = normalizeMatchSource(matchSource);
        this.httpClient = httpClient;
    }

    @Override
    public Optional<MatchRecommendationResponse> scoreMatch(
            String systemPrompt,
            String userPrompt,
            Long targetId,
            int fallbackScore,
            MatchType matchType
    ) {
        if (!isConfigured()) {
            return Optional.empty();
        }

        String webhookUrl = matchType == MatchType.MANAGER_APPLICANT
                ? managerMatchWebhookUrl
                : workerMatchWebhookUrl;

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("systemPrompt", systemPrompt);
            body.put("userPrompt", userPrompt);
            body.put("targetId", targetId);
            body.put("fallbackScore", fallbackScore);
            body.put("matchType", matchType.name());
            body.put("requiredSource", matchSource);
            body.put("schema", List.of(
                    "targetId", "aiScore", "fallbackScore", "label", "explanation",
                    "strengths", "risks", "recommendedAction", "source"
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
                log.warn("n8n matching webhook returned HTTP {} for {}", response.statusCode(), matchType);
                return Optional.empty();
            }
            if (!hasRequiredSource(response.body())) {
                log.warn("n8n matching webhook did not return the required DeepSeek source for {}", matchType);
                return Optional.empty();
            }

            Optional<MatchRecommendationResponse> parsed = parser.parse(
                    response.body(), targetId, fallbackScore, matchSource
            );
            if (parsed.isEmpty()) {
                log.warn("n8n matching webhook returned an invalid response body for {}", matchType);
            }
            return parsed;
        } catch (Exception exception) {
            log.warn("n8n matching webhook call failed for {}: {}", matchType, exception.toString());
            return Optional.empty();
        }
    }

    private boolean isConfigured() {
        return matchingEnabled
                && workerMatchWebhookUrl != null
                && !workerMatchWebhookUrl.isBlank()
                && managerMatchWebhookUrl != null
                && !managerMatchWebhookUrl.isBlank();
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
