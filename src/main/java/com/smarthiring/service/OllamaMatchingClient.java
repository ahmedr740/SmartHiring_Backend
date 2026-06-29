package com.smarthiring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthiring.dto.MatchRecommendationResponse;
import com.smarthiring.dto.MatchType;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class OllamaMatchingClient implements ExternalMatchingClient {

    private static final String SOURCE = "OLLAMA";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final MatchRecommendationJsonParser parser;
    private final boolean matchingEnabled;
    private final String baseUrl;
    private final String model;

    public OllamaMatchingClient(
            boolean matchingEnabled,
            String baseUrl,
            String model
    ) {
        this(
                matchingEnabled,
                baseUrl,
                model,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .build(),
                new ObjectMapper(),
                new MatchRecommendationJsonParser()
        );
    }

    OllamaMatchingClient(
            boolean matchingEnabled,
            String baseUrl,
            String model,
            HttpClient httpClient,
            ObjectMapper objectMapper,
            MatchRecommendationJsonParser parser
    ) {
        this.matchingEnabled = matchingEnabled;
        this.baseUrl = baseUrl;
        this.model = model;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.parser = parser;
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

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("prompt", buildPrompt(systemPrompt, userPrompt, targetId, fallbackScore, matchType));
            body.put("stream", false);
            body.put("format", "json");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl.replaceAll("/$", "") + "/api/generate"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return Optional.empty();
            }

            return parser.parse(response.body(), targetId, fallbackScore, SOURCE);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private boolean isConfigured() {
        return matchingEnabled && baseUrl != null && !baseUrl.isBlank() && model != null && !model.isBlank();
    }

    private String buildPrompt(String systemPrompt, String userPrompt, Long targetId, int fallbackScore, MatchType matchType) {
        String task = matchType == MatchType.MANAGER_APPLICANT
                ? "Rank this applicant for the manager's shift."
                : "Rank this shift for the worker.";

        return """
                %s

                Task: %s
                %s

                Return JSON only with this shape:
                {"targetId": %d, "aiScore": 0-100, "label": "Strong match|Good match|Possible match", "explanation": "...", "strengths": ["..."], "risks": ["..."], "recommendedAction": "..."}

                Use the local fallback score %d as a baseline, but adjust based on fit.
                Do not mention protected traits or data that was not supplied.
                """.formatted(systemPrompt, task, userPrompt, targetId, fallbackScore);
    }
}
