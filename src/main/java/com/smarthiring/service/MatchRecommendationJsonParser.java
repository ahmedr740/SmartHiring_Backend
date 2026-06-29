package com.smarthiring.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthiring.dto.MatchRecommendationResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class MatchRecommendationJsonParser {

    private final ObjectMapper objectMapper;

    MatchRecommendationJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    MatchRecommendationJsonParser() {
        this(new ObjectMapper());
    }

    Optional<MatchRecommendationResponse> parse(String responseBody, Long targetId, int fallbackScore, String source) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode node = unwrap(root);
            return Optional.of(toRecommendation(node, targetId, fallbackScore, source));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private JsonNode unwrap(JsonNode root) {
        if (root.has("response") && root.get("response").isTextual()) {
            try {
                return objectMapper.readTree(root.get("response").asText());
            } catch (Exception ignored) {
                return root;
            }
        }
        if (root.isArray() && root.size() > 0) {
            return root.get(0);
        }
        return root;
    }

    private MatchRecommendationResponse toRecommendation(JsonNode node, Long targetId, int fallbackScore, String source) {
        MatchRecommendationResponse recommendation = new MatchRecommendationResponse();
        recommendation.setTargetId(node.path("targetId").asLong(targetId));
        recommendation.setAiScore(clampScore(node.path("aiScore").asInt(node.path("score").asInt(fallbackScore))));
        recommendation.setFallbackScore(fallbackScore);
        recommendation.setLabel(cleanText(node.path("label").asText(labelForScore(recommendation.getAiScore()))));
        recommendation.setExplanation(cleanText(node.path("explanation").asText("Matched using local AI.")));
        recommendation.setStrengths(readStringList(node.path("strengths")));
        recommendation.setRisks(readStringList(node.path("risks")));
        recommendation.setRecommendedAction(cleanText(node.path("recommendedAction").asText("Review this match.")));
        recommendation.setGeneratedAt(LocalDateTime.now());
        recommendation.setSource(source);
        return recommendation;
    }

    private List<String> readStringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                String value = cleanText(item.asText(""));
                if (!value.isBlank()) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    private String cleanText(String value) {
        return value == null ? "" : value.trim();
    }

    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private String labelForScore(int score) {
        if (score >= 80) {
            return "Strong match";
        }
        if (score >= 60) {
            return "Good match";
        }
        return "Possible match";
    }
}
