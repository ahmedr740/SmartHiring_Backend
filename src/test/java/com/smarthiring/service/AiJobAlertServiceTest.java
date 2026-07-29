package com.smarthiring.service;

import com.smarthiring.dto.MatchRecommendationResponse;
import com.smarthiring.model.Shift;
import com.smarthiring.model.User;
import com.smarthiring.repository.ShiftRepository;
import com.smarthiring.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiJobAlertServiceTest {

    private final ShiftRepository shiftRepository = mock(ShiftRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final MatchingService matchingService = mock(MatchingService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final AiJobAlertService service = new AiJobAlertService(
            shiftRepository,
            userRepository,
            matchingService,
            notificationService
    );

    @Test
    void createsAlertOnlyForGenuineStrongAiScore() {
        Shift shift = new Shift();
        shift.setId(10L);
        shift.setTitle("Dinner Waiter");
        shift.setLocation("Central");
        shift.setStatus("OPEN");
        User worker = new User();
        worker.setId(1L);

        when(shiftRepository.findById(10L)).thenReturn(Optional.of(shift));
        when(userRepository.findAllByRoleIgnoreCaseAndStatusIgnoreCase("WORKER", "ACTIVE"))
                .thenReturn(List.of(worker));
        when(matchingService.recommendWorkerShift(worker, shift)).thenReturn(new MatchRecommendationResponse(
                10L, null, 88, 70, "Strong match", "Strong fit",
                List.of("Skills"), List.of(), "Apply", LocalDateTime.now(), "N8N_DEEPSEEK"
        ));

        service.scanNewShift(10L);

        verify(notificationService).create(
                eq(worker),
                eq("AI_JOB_MATCH"),
                anyString(),
                contains("88%"),
                eq("/worker-matches"),
                eq(true),
                eq("ai-job-match:1:10")
        );
    }

    @Test
    void doesNotCallFallbackScoreAnAiAlert() {
        Shift shift = new Shift();
        shift.setId(10L);
        shift.setStatus("OPEN");
        User worker = new User();
        worker.setId(1L);

        when(shiftRepository.findById(10L)).thenReturn(Optional.of(shift));
        when(userRepository.findAllByRoleIgnoreCaseAndStatusIgnoreCase("WORKER", "ACTIVE"))
                .thenReturn(List.of(worker));
        when(matchingService.recommendWorkerShift(worker, shift)).thenReturn(new MatchRecommendationResponse(
                10L, null, null, 90, "Strong match", "Fallback",
                List.of(), List.of(), "Apply", LocalDateTime.now(), "FALLBACK"
        ));

        service.scanNewShift(10L);

        verifyNoInteractions(notificationService);
    }
}
