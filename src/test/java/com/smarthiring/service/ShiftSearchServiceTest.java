package com.smarthiring.service;

import com.smarthiring.dto.ShiftSearchMatch;
import com.smarthiring.dto.ShiftSearchResponse;
import com.smarthiring.model.Shift;
import com.smarthiring.model.User;
import com.smarthiring.repository.ShiftRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShiftSearchServiceTest {

    @Test
    void rejectsNonWorkers() {
        ExternalShiftSearchClient client = mock(ExternalShiftSearchClient.class);
        ShiftRepository shiftRepository = mock(ShiftRepository.class);
        ShiftSearchService service = new ShiftSearchService(client, shiftRepository);

        assertThatThrownBy(() -> service.searchShifts("waiter shifts Friday night", manager()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only workers");
    }

    @Test
    void rejectsBlankQuery() {
        ExternalShiftSearchClient client = mock(ExternalShiftSearchClient.class);
        ShiftRepository shiftRepository = mock(ShiftRepository.class);
        ShiftSearchService service = new ShiftSearchService(client, shiftRepository);

        assertThatThrownBy(() -> service.searchShifts("   ", worker()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Describe the kind of shift");
    }

    @Test
    void rejectsOverlyLongQuery() {
        ExternalShiftSearchClient client = mock(ExternalShiftSearchClient.class);
        ShiftRepository shiftRepository = mock(ShiftRepository.class);
        ShiftSearchService service = new ShiftSearchService(client, shiftRepository);
        String longQuery = "a".repeat(501);

        assertThatThrownBy(() -> service.searchShifts(longQuery, worker()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("500 characters");
    }

    @Test
    void returnsEmptyResponseWithoutCallingClientWhenNoOpenShifts() {
        ExternalShiftSearchClient client = mock(ExternalShiftSearchClient.class);
        ShiftRepository shiftRepository = mock(ShiftRepository.class);
        when(shiftRepository.findAllByStatusIgnoreCase("OPEN")).thenReturn(List.of());
        ShiftSearchService service = new ShiftSearchService(client, shiftRepository);

        ShiftSearchResponse response = service.searchShifts("waiter shifts Friday night", worker());

        assertThat(response.getMatches()).isEmpty();
        assertThat(response.getInterpretation()).contains("no open shifts");
        verify(client, never()).searchShifts(any(), any(), any());
    }

    @Test
    void returnsResultsFromClientWithAllowedShiftIds() {
        ExternalShiftSearchClient client = mock(ExternalShiftSearchClient.class);
        ShiftRepository shiftRepository = mock(ShiftRepository.class);
        Shift shift = new Shift();
        shift.setId(7L);
        shift.setTitle("Friday Night Server");
        shift.setRoleNeeded("WAITER");
        when(shiftRepository.findAllByStatusIgnoreCase("OPEN")).thenReturn(List.of(shift));

        ShiftSearchResponse expected = new ShiftSearchResponse(
                "Waiter shifts Friday night",
                List.of(new ShiftSearchMatch(7L, "Matches role and timing")),
                null,
                "OLLAMA"
        );
        when(client.searchShifts(anyString(), anyString(), anySet())).thenReturn(Optional.of(expected));

        ShiftSearchService service = new ShiftSearchService(client, shiftRepository);
        ShiftSearchResponse response = service.searchShifts("waiter shifts Friday night", worker());

        assertThat(response.getMatches()).hasSize(1);
        assertThat(response.getMatches().get(0).getShiftId()).isEqualTo(7L);
        verify(client).searchShifts(anyString(), anyString(), eq(Set.of(7L)));
    }

    @Test
    void throws503WhenClientUnavailable() {
        ExternalShiftSearchClient client = mock(ExternalShiftSearchClient.class);
        ShiftRepository shiftRepository = mock(ShiftRepository.class);
        Shift shift = new Shift();
        shift.setId(1L);
        when(shiftRepository.findAllByStatusIgnoreCase("OPEN")).thenReturn(List.of(shift));
        when(client.searchShifts(any(), any(), any())).thenReturn(Optional.empty());

        ShiftSearchService service = new ShiftSearchService(client, shiftRepository);

        assertThatThrownBy(() -> service.searchShifts("waiter shifts", worker()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("unavailable");
    }

    private User worker() {
        User user = new User();
        user.setRole("WORKER");
        return user;
    }

    private User manager() {
        User user = new User();
        user.setRole("MANAGER");
        return user;
    }
}
