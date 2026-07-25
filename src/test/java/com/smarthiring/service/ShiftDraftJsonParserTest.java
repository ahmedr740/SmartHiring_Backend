package com.smarthiring.service;

import com.smarthiring.dto.ShiftDraftResponse;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ShiftDraftJsonParserTest {

    private final ShiftDraftJsonParser parser = new ShiftDraftJsonParser();

    @Test
    void parsesDirectJsonResponse() {
        Optional<ShiftDraftResponse> draft = parser.parse("""
                {
                  "title": "Friday Night Server",
                  "description": "Two servers needed for a busy Friday night.",
                  "requirements": "Available Friday evening",
                  "roleNeeded": "waiter",
                  "pay": 18.5,
                  "date": "2026-07-24",
                  "startTime": "18:00",
                  "endTime": "23:00",
                  "location": "Downtown",
                  "assumptions": "Assumed the upcoming Friday."
                }
                """, "OLLAMA");

        assertThat(draft).isPresent();
        assertThat(draft.get().getTitle()).isEqualTo("Friday Night Server");
        assertThat(draft.get().getRoleNeeded()).isEqualTo("WAITER");
        assertThat(draft.get().getPay()).isEqualTo(18.5);
        assertThat(draft.get().getDate()).isEqualTo("2026-07-24");
        assertThat(draft.get().getStartTime()).isEqualTo("18:00");
        assertThat(draft.get().getSource()).isEqualTo("OLLAMA");
    }

    @Test
    void unwrapsOllamaResponseWrapper() {
        Optional<ShiftDraftResponse> draft = parser.parse("""
                {"response": "{\\"title\\":\\"Barista Shift\\",\\"roleNeeded\\":\\"BARISTA\\"}", "done": true}
                """, "OLLAMA");

        assertThat(draft).isPresent();
        assertThat(draft.get().getTitle()).isEqualTo("Barista Shift");
        assertThat(draft.get().getRoleNeeded()).isEqualTo("BARISTA");
    }

    @Test
    void blanksOutInvalidRoleDateAndTime() {
        Optional<ShiftDraftResponse> draft = parser.parse("""
                {
                  "title": "Shift",
                  "roleNeeded": "MANAGER",
                  "date": "next Friday",
                  "startTime": "6pm",
                  "pay": -5
                }
                """, "OLLAMA");

        assertThat(draft).isPresent();
        assertThat(draft.get().getRoleNeeded()).isEmpty();
        assertThat(draft.get().getDate()).isEmpty();
        assertThat(draft.get().getStartTime()).isEmpty();
        assertThat(draft.get().getPay()).isNull();
    }

    @Test
    void returnsEmptyForMalformedJson() {
        Optional<ShiftDraftResponse> draft = parser.parse("not json at all", "OLLAMA");

        assertThat(draft).isEmpty();
    }
}
