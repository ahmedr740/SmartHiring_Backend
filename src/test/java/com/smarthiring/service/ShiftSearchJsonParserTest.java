package com.smarthiring.service;

import com.smarthiring.dto.ShiftSearchResponse;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ShiftSearchJsonParserTest {

    private final ShiftSearchJsonParser parser = new ShiftSearchJsonParser();

    @Test
    void parsesDirectJsonResponse() {
        Optional<ShiftSearchResponse> result = parser.parse("""
                {
                  "interpretation": "Looking for waiter shifts on Friday evening.",
                  "matches": [
                    {"shiftId": 1, "reason": "Friday evening waiter shift"},
                    {"shiftId": 2, "reason": "Close match on timing"}
                  ]
                }
                """, Set.of(1L, 2L, 3L), "OLLAMA");

        assertThat(result).isPresent();
        assertThat(result.get().getInterpretation()).isEqualTo("Looking for waiter shifts on Friday evening.");
        assertThat(result.get().getMatches()).hasSize(2);
        assertThat(result.get().getMatches().get(0).getShiftId()).isEqualTo(1L);
        assertThat(result.get().getSource()).isEqualTo("OLLAMA");
    }

    @Test
    void unwrapsOllamaResponseWrapper() {
        Optional<ShiftSearchResponse> result = parser.parse("""
                {"response": "{\\"interpretation\\":\\"Barista shifts\\",\\"matches\\":[{\\"shiftId\\":5,\\"reason\\":\\"Barista role\\"}]}", "done": true}
                """, Set.of(5L), "OLLAMA");

        assertThat(result).isPresent();
        assertThat(result.get().getInterpretation()).isEqualTo("Barista shifts");
        assertThat(result.get().getMatches()).hasSize(1);
        assertThat(result.get().getMatches().get(0).getShiftId()).isEqualTo(5L);
    }

    @Test
    void dropsShiftIdsNotInAllowedSet() {
        Optional<ShiftSearchResponse> result = parser.parse("""
                {"interpretation": "test", "matches": [{"shiftId": 1}, {"shiftId": 999}]}
                """, Set.of(1L), "OLLAMA");

        assertThat(result).isPresent();
        assertThat(result.get().getMatches()).hasSize(1);
        assertThat(result.get().getMatches().get(0).getShiftId()).isEqualTo(1L);
    }

    @Test
    void dedupesRepeatedShiftIds() {
        StringBuilder matches = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            if (i > 0) matches.append(",");
            matches.append("{\"shiftId\": 1}");
        }
        Optional<ShiftSearchResponse> result = parser.parse(
                "{\"interpretation\": \"test\", \"matches\": [" + matches + ", {\"shiftId\": 2}]}",
                Set.of(1L, 2L),
                "OLLAMA"
        );

        assertThat(result).isPresent();
        assertThat(result.get().getMatches()).hasSize(2);
    }

    @Test
    void capsAtTwelveMatches() {
        Set<Long> allowed = new java.util.HashSet<>();
        StringBuilder matches = new StringBuilder();
        for (long i = 1; i <= 20; i++) {
            allowed.add(i);
            if (i > 1) matches.append(",");
            matches.append("{\"shiftId\": ").append(i).append("}");
        }
        Optional<ShiftSearchResponse> result = parser.parse(
                "{\"interpretation\": \"test\", \"matches\": [" + matches + "]}",
                allowed,
                "OLLAMA"
        );

        assertThat(result).isPresent();
        assertThat(result.get().getMatches()).hasSize(12);
    }

    @Test
    void returnsEmptyForMalformedJson() {
        Optional<ShiftSearchResponse> result = parser.parse("not json", Set.of(1L), "OLLAMA");

        assertThat(result).isEmpty();
    }
}
