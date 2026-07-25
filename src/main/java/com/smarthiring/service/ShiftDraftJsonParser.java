package com.smarthiring.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthiring.dto.ShiftDraftResponse;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

final class ShiftDraftJsonParser {

    private static final Set<String> ALLOWED_ROLES = Set.of(
            "WAITER", "CHEF", "BARISTA", "CASHIER", "KITCHEN HELPER"
    );
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern TIME_PATTERN = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    private final ObjectMapper objectMapper;

    ShiftDraftJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    ShiftDraftJsonParser() {
        this(new ObjectMapper());
    }

    Optional<ShiftDraftResponse> parse(String responseBody, String source) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode node = unwrap(root);
            return Optional.of(toDraft(node, source));
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

    private ShiftDraftResponse toDraft(JsonNode node, String source) {
        ShiftDraftResponse draft = new ShiftDraftResponse();
        draft.setTitle(cleanText(node.path("title").asText("")));
        draft.setDescription(cleanText(node.path("description").asText("")));
        draft.setRequirements(cleanText(node.path("requirements").asText("")));
        draft.setRoleNeeded(cleanRole(node.path("roleNeeded").asText("")));
        draft.setPay(cleanPay(node.path("pay")));
        draft.setDate(cleanPattern(node.path("date").asText(""), DATE_PATTERN));
        draft.setStartTime(cleanPattern(node.path("startTime").asText(""), TIME_PATTERN));
        draft.setEndTime(cleanPattern(node.path("endTime").asText(""), TIME_PATTERN));
        draft.setLocation(cleanText(node.path("location").asText("")));
        draft.setAssumptions(cleanText(node.path("assumptions").asText("")));
        draft.setGeneratedAt(LocalDateTime.now());
        draft.setSource(source);
        return draft;
    }

    private String cleanRole(String value) {
        String normalized = cleanText(value).toUpperCase();
        return ALLOWED_ROLES.contains(normalized) ? normalized : "";
    }

    private Double cleanPay(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        double value = node.asDouble(-1);
        return value > 0 ? value : null;
    }

    private String cleanPattern(String value, Pattern pattern) {
        String trimmed = cleanText(value);
        return pattern.matcher(trimmed).matches() ? trimmed : "";
    }

    private String cleanText(String value) {
        return value == null ? "" : value.trim();
    }
}
