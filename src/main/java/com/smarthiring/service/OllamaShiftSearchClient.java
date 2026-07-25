package com.smarthiring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthiring.dto.ShiftSearchResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class OllamaShiftSearchClient implements ExternalShiftSearchClient {

    private static final String SOURCE = "OLLAMA";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ShiftSearchJsonParser parser;
    private final boolean enabled;
    private final String baseUrl;
    private final String model;

    public OllamaShiftSearchClient(boolean enabled, String baseUrl, String model) {
        this(
                enabled,
                baseUrl,
                model,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .build(),
                new ObjectMapper(),
                new ShiftSearchJsonParser()
        );
    }

    OllamaShiftSearchClient(
            boolean enabled,
            String baseUrl,
            String model,
            HttpClient httpClient,
            ObjectMapper objectMapper,
            ShiftSearchJsonParser parser
    ) {
        this.enabled = enabled;
        this.baseUrl = baseUrl;
        this.model = model;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.parser = parser;
    }

    @Override
    public Optional<ShiftSearchResponse> searchShifts(String systemPrompt, String userPrompt, Set<Long> allowedShiftIds) {
        if (!isConfigured()) {
            return Optional.empty();
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("prompt", systemPrompt + "\n\n" + userPrompt);
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

            return parser.parse(response.body(), allowedShiftIds, SOURCE);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private boolean isConfigured() {
        return enabled && baseUrl != null && !baseUrl.isBlank() && model != null && !model.isBlank();
    }
}
