package com.smarthiring.service;

import com.smarthiring.dto.ShiftDraftResponse;
import com.smarthiring.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShiftDraftServiceTest {

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-07-24T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void rejectsNonManagers() {
        ExternalShiftDraftClient client = mock(ExternalShiftDraftClient.class);
        ShiftDraftService service = new ShiftDraftService(client, fixedClock);
        User worker = activeUser("WORKER");

        assertThatThrownBy(() -> service.draftShift("need 2 servers Friday night", worker))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only managers");
    }

    @Test
    void rejectsInactiveManagers() {
        ExternalShiftDraftClient client = mock(ExternalShiftDraftClient.class);
        ShiftDraftService service = new ShiftDraftService(client, fixedClock);
        User manager = activeUser("MANAGER");
        manager.setStatus("PENDING");

        assertThatThrownBy(() -> service.draftShift("need 2 servers Friday night", manager))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not approved");
    }

    @Test
    void rejectsBlankInput() {
        ExternalShiftDraftClient client = mock(ExternalShiftDraftClient.class);
        ShiftDraftService service = new ShiftDraftService(client, fixedClock);

        assertThatThrownBy(() -> service.draftShift("   ", activeUser("MANAGER")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Describe the shift");
    }

    @Test
    void rejectsOverlyLongInput() {
        ExternalShiftDraftClient client = mock(ExternalShiftDraftClient.class);
        ShiftDraftService service = new ShiftDraftService(client, fixedClock);
        String longInput = "a".repeat(2001);

        assertThatThrownBy(() -> service.draftShift(longInput, activeUser("MANAGER")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("2000 characters");
    }

    @Test
    void returnsDraftWhenClientSucceeds() {
        ExternalShiftDraftClient client = mock(ExternalShiftDraftClient.class);
        ShiftDraftResponse expected = new ShiftDraftResponse();
        expected.setTitle("Friday Night Server");
        when(client.draftShift(anyString(), anyString())).thenReturn(Optional.of(expected));

        ShiftDraftService service = new ShiftDraftService(client, fixedClock);
        ShiftDraftResponse draft = service.draftShift("need 2 servers Fri night, $18/hr", activeUser("MANAGER"));

        assertThat(draft.getTitle()).isEqualTo("Friday Night Server");
        verify(client).draftShift(anyString(), org.mockito.ArgumentMatchers.contains("2026-07-24"));
    }

    @Test
    void throws503WhenClientUnavailable() {
        ExternalShiftDraftClient client = mock(ExternalShiftDraftClient.class);
        when(client.draftShift(any(), any())).thenReturn(Optional.empty());
        ShiftDraftService service = new ShiftDraftService(client, fixedClock);

        assertThatThrownBy(() -> service.draftShift("need 2 servers Friday night", activeUser("MANAGER")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("unavailable");
    }

    private User activeUser(String role) {
        User user = new User();
        user.setRole(role);
        user.setStatus("ACTIVE");
        return user;
    }
}
