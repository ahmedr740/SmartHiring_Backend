package com.smarthiring.service;

import com.smarthiring.dto.MatchRecommendationResponse;
import com.smarthiring.dto.MatchType;
import com.smarthiring.model.Application;
import com.smarthiring.model.Shift;
import com.smarthiring.model.User;
import com.smarthiring.repository.AiMatchCacheRepository;
import com.smarthiring.repository.ApplicationRepository;
import com.smarthiring.repository.ShiftRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MatchingServiceTest {

    private final ShiftRepository shiftRepository = mock(ShiftRepository.class);
    private final ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
    private final AiMatchCacheRepository cacheRepository = mock(AiMatchCacheRepository.class);
    private final ExternalMatchingClient externalMatchingClient = mock(ExternalMatchingClient.class);
    private final MatchingService matchingService = new MatchingService(
            shiftRepository,
            applicationRepository,
            cacheRepository,
            externalMatchingClient
    );

    @Test
    void fallsBackWhenN8nIsUnavailable() {
        User worker = worker();
        Shift shift = shift();

        when(shiftRepository.findAll()).thenReturn(List.of(shift));
        when(cacheRepository.findByCacheKey(anyString())).thenReturn(Optional.empty());
        when(externalMatchingClient.scoreMatch(anyString(), anyString(), anyLong(), anyInt(), any(MatchType.class))).thenReturn(Optional.empty());

        List<MatchRecommendationResponse> recommendations = matchingService.getWorkerShiftMatches(worker);

        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).getSource()).isEqualTo("FALLBACK");
        assertThat(recommendations.get(0).getAiScore()).isNull();
        assertThat(recommendations.get(0).getFallbackScore()).isGreaterThan(0);
    }

    @Test
    void usesN8nRecommendationWhenAvailable() {
        User worker = worker();
        Shift shift = shift();
        MatchRecommendationResponse aiRecommendation = new MatchRecommendationResponse(
                shift.getId(),
                null,
                91,
                80,
                "Strong match",
                "The worker has directly relevant service experience.",
                List.of("Relevant skills"),
                List.of("Confirm availability"),
                "Invite the worker to apply.",
                LocalDateTime.now(),
                "N8N_OLLAMA"
        );

        when(shiftRepository.findAll()).thenReturn(List.of(shift));
        when(cacheRepository.findByCacheKey(anyString())).thenReturn(Optional.empty());
        when(cacheRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(externalMatchingClient.scoreMatch(anyString(), anyString(), eq(shift.getId()), anyInt(), eq(MatchType.WORKER_SHIFT)))
                .thenReturn(Optional.of(aiRecommendation));

        List<MatchRecommendationResponse> recommendations = matchingService.getWorkerShiftMatches(worker);

        assertThat(recommendations.get(0).getSource()).isEqualTo("N8N_OLLAMA");
        assertThat(recommendations.get(0).getAiScore()).isEqualTo(91);
        assertThat(recommendations.get(0).getRank()).isEqualTo(1);
    }

    @Test
    void n8nPromptDoesNotIncludeSensitiveUserFields() {
        User worker = worker();
        worker.setEmail("worker@example.com");
        worker.setPassword("super-secret-password");
        Shift shift = shift();

        when(shiftRepository.findAll()).thenReturn(List.of(shift));
        when(cacheRepository.findByCacheKey(anyString())).thenReturn(Optional.empty());
        when(externalMatchingClient.scoreMatch(anyString(), anyString(), anyLong(), anyInt(), any(MatchType.class))).thenReturn(Optional.empty());

        matchingService.getWorkerShiftMatches(worker);

        verify(externalMatchingClient).scoreMatch(anyString(), argThat(prompt ->
                !prompt.contains("super-secret-password") && !prompt.contains("worker@example.com")
        ), anyLong(), anyInt(), eq(MatchType.WORKER_SHIFT));
    }

    @Test
    void rejectsManagerApplicantMatchesForNonOwningManager() {
        User owner = manager(100L);
        User otherManager = manager(200L);
        Shift shift = shift();
        shift.setManager(owner);

        when(shiftRepository.findById(shift.getId())).thenReturn(Optional.of(shift));

        assertThatThrownBy(() -> matchingService.getManagerApplicantMatches(shift.getId(), otherManager))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void calculatesFallbackScoreFromSkillsLocationRatingAndCompletions() {
        int score = matchingService.scoreWorkerShift(worker(), shift());

        assertThat(score).isGreaterThanOrEqualTo(80);
    }

    private User worker() {
        User worker = new User();
        worker.setId(1L);
        worker.setName("Ava Worker");
        worker.setRole("WORKER");
        worker.setStatus("ACTIVE");
        worker.setSkills("waiter, cashier");
        worker.setLocation("Central");
        worker.setAvailability("Weeknights");
        worker.setRating(4.5d);
        worker.setCompletedShiftsCount(6);
        return worker;
    }

    private User manager(Long id) {
        User manager = new User();
        manager.setId(id);
        manager.setRole("MANAGER");
        manager.setStatus("ACTIVE");
        manager.setName("Manager " + id);
        return manager;
    }

    private Shift shift() {
        Shift shift = new Shift();
        shift.setId(10L);
        shift.setTitle("Dinner waiter");
        shift.setRoleNeeded("Waiter");
        shift.setDate("2026-06-20");
        shift.setStartTime("18:00");
        shift.setEndTime("22:00");
        shift.setLocation("Central");
        shift.setPay(18d);
        shift.setStatus("OPEN");
        shift.setManager(manager(100L));
        return shift;
    }

    @SuppressWarnings("unused")
    private Application application() {
        Application application = new Application();
        application.setId(50L);
        application.setWorker(worker());
        application.setShift(shift());
        application.setStatus("PENDING");
        return application;
    }
}
