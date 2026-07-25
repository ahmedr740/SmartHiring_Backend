package com.smarthiring.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthiring.dto.ShiftSearchMatch;
import com.smarthiring.dto.ShiftSearchResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class ShiftSearchJsonParser {

    private static final int MAX_MATCHES = 12;

    private final ObjectMapper objectMapper;

    ShiftSearchJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    ShiftSearchJsonParser() {
        this(new ObjectMapper());
    }

    Optional<ShiftSearchResponse> parse(String responseBody, Set<Long> allowedShiftIds, String source) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode node = unwrap(root);
            return Optional.of(toResponse(node, allowedShiftIds, source));
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

    private ShiftSearchResponse toResponse(JsonNode node, Set<Long> allowedShiftIds, String source) {
        List<ShiftSearchMatch> matches = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        JsonNode matchesNode = node.path("matches");
        if (matchesNode.isArray()) {
            for (JsonNode matchNode : matchesNode) {
                if (matches.size() >= MAX_MATCHES) {
                    break;
                }
                long shiftId = matchNode.path("shiftId").asLong(-1);
                if (shiftId <= 0 || !allowedShiftIds.contains(shiftId) || !seen.add(shiftId)) {
                    continue;
                }
                matches.add(new ShiftSearchMatch(shiftId, cleanText(matchNode.path("reason").asText(""))));
            }
        }

        ShiftSearchResponse response = new ShiftSearchResponse();
        response.setInterpretation(cleanText(node.path("interpretation").asText("")));
        response.setMatches(matches);
        response.setGeneratedAt(LocalDateTime.now());
        response.setSource(source);
        return response;
    }

    private String cleanText(String value) {
        return value == null ? "" : value.trim();
    }
}
