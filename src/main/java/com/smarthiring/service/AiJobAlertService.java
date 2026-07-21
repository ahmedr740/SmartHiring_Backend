package com.smarthiring.service;

import com.smarthiring.dto.MatchRecommendationResponse;
import com.smarthiring.model.Shift;
import com.smarthiring.model.User;
import com.smarthiring.repository.ShiftRepository;
import com.smarthiring.repository.UserRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AiJobAlertService {

    private final ShiftRepository shiftRepository;
    private final UserRepository userRepository;
    private final MatchingService matchingService;
    private final NotificationService notificationService;

    public AiJobAlertService(
            ShiftRepository shiftRepository,
            UserRepository userRepository,
            MatchingService matchingService,
            NotificationService notificationService
    ) {
        this.shiftRepository = shiftRepository;
        this.userRepository = userRepository;
        this.matchingService = matchingService;
        this.notificationService = notificationService;
    }

    @Async
    public void scanNewShift(Long shiftId) {
        Shift shift = shiftRepository.findById(shiftId).orElse(null);
        if (shift == null || !"OPEN".equalsIgnoreCase(shift.getStatus())) {
            return;
        }

        for (User worker : userRepository.findAllByRoleIgnoreCaseAndStatusIgnoreCase("WORKER", "ACTIVE")) {
            MatchRecommendationResponse recommendation = matchingService.recommendWorkerShift(worker, shift);
            boolean genuineAi = recommendation.getAiScore() != null
                    && recommendation.getSource() != null
                    && !"FALLBACK".equalsIgnoreCase(recommendation.getSource());
            if (genuineAi && recommendation.getAiScore() >= 80) {
                notificationService.create(
                        worker,
                        "AI_JOB_MATCH",
                        "Strong job match found",
                        "%s at %s scored %d%% for your profile.".formatted(
                                shift.getTitle(),
                                shift.getLocation(),
                                recommendation.getAiScore()
                        ),
                        "/worker-matches",
                        true,
                        "ai-job-match:%d:%d".formatted(worker.getId(), shift.getId())
                );
            }
        }
    }
}
