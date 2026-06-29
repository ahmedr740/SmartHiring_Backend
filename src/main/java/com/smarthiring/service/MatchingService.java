package com.smarthiring.service;

import com.smarthiring.dto.MatchRecommendationResponse;
import com.smarthiring.dto.MatchType;
import com.smarthiring.model.AiMatchCache;
import com.smarthiring.model.Application;
import com.smarthiring.model.Shift;
import com.smarthiring.model.User;
import com.smarthiring.repository.AiMatchCacheRepository;
import com.smarthiring.repository.ApplicationRepository;
import com.smarthiring.repository.ShiftRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class MatchingService {

    private static final String SYSTEM_PROMPT = """
            You rank restaurant staffing matches for Smart Hiring.
            Use only the supplied shift, worker, manager, rating, location, skill, and completion data.
            Do not infer protected traits. Do not mention private data that is not supplied.
            Return practical, concise hiring guidance for a restaurant shift marketplace.
            """;

    private final ShiftRepository shiftRepository;
    private final ApplicationRepository applicationRepository;
    private final AiMatchCacheRepository cacheRepository;
    private final ExternalMatchingClient externalMatchingClient;

    public MatchingService(
            ShiftRepository shiftRepository,
            ApplicationRepository applicationRepository,
            AiMatchCacheRepository cacheRepository,
            ExternalMatchingClient externalMatchingClient
    ) {
        this.shiftRepository = shiftRepository;
        this.applicationRepository = applicationRepository;
        this.cacheRepository = cacheRepository;
        this.externalMatchingClient = externalMatchingClient;
    }

    public List<MatchRecommendationResponse> getWorkerShiftMatches(User worker) {
        if (!isWorker(worker)) {
            throw new ResponseStatusException(FORBIDDEN, "Only workers can view shift recommendations");
        }

        List<MatchRecommendationResponse> recommendations = shiftRepository.findAll().stream()
                .filter(shift -> "OPEN".equalsIgnoreCase(shift.getStatus()))
                .map(shift -> recommendWorkerShift(worker, shift))
                .sorted(scoreComparator())
                .toList();

        return rank(recommendations);
    }

    public List<MatchRecommendationResponse> getManagerApplicantMatches(Long shiftId, User currentUser) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Shift not found"));

        boolean canView = isAdmin(currentUser)
                || (isManager(currentUser) && shift.getManager() != null && shift.getManager().getId().equals(currentUser.getId()));
        if (!canView) {
            throw new ResponseStatusException(FORBIDDEN, "You do not have permission to view applicant recommendations");
        }

        List<MatchRecommendationResponse> recommendations = applicationRepository.findAllByShiftId(shiftId).stream()
                .map(application -> recommendApplicant(shift, application))
                .sorted(scoreComparator())
                .toList();

        return rank(recommendations);
    }

    MatchRecommendationResponse recommendWorkerShift(User worker, Shift shift) {
        int fallbackScore = scoreWorkerShift(worker, shift);
        String cacheKey = "worker-shift:%d:%d".formatted(worker.getId(), shift.getId());

        return getCachedRecommendation(cacheKey, fallbackScore)
                .orElseGet(() -> externalMatchingClient
                        .scoreMatch(SYSTEM_PROMPT, workerShiftPrompt(worker, shift), shift.getId(), fallbackScore, MatchType.WORKER_SHIFT)
                        .map(recommendation -> cache(cacheKey, recommendation))
                        .orElseGet(() -> fallback(shift.getId(), fallbackScore, "Apply if the schedule and location work for you.")));
    }

    MatchRecommendationResponse recommendApplicant(Shift shift, Application application) {
        if (application.getWorker() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Application is missing worker details");
        }

        int fallbackScore = scoreWorkerShift(application.getWorker(), shift);
        String cacheKey = "manager-applicant:%d:%d".formatted(shift.getId(), application.getId());

        return getCachedRecommendation(cacheKey, fallbackScore)
                .orElseGet(() -> externalMatchingClient
                        .scoreMatch(SYSTEM_PROMPT, applicantPrompt(shift, application), application.getId(), fallbackScore, MatchType.MANAGER_APPLICANT)
                        .map(recommendation -> cache(cacheKey, recommendation))
                        .orElseGet(() -> fallback(application.getId(), fallbackScore, "Review this applicant's skills and availability.")));
    }

    int scoreWorkerShift(User worker, Shift shift) {
        String roleNeedle = lower(firstPresent(shift.getRoleNeeded(), shift.getTitle()));
        boolean roleMatch = splitTerms(worker.getSkills()).stream()
                .anyMatch(skill -> roleNeedle.contains(skill) || skill.contains(roleNeedle));

        String workerLocation = lower(worker.getLocation());
        String shiftLocation = lower(shift.getLocation());
        boolean locationMatch = !workerLocation.isBlank()
                && !shiftLocation.isBlank()
                && (workerLocation.contains(shiftLocation) || shiftLocation.contains(workerLocation));

        double ratingScore = Math.min(((worker.getRating() == null ? 0d : worker.getRating()) / 5d) * 20d, 20d);
        int completionScore = Math.min((worker.getCompletedShiftsCount() == null ? 0 : worker.getCompletedShiftsCount()) * 2, 10);

        return (int) Math.round((roleMatch ? 45 : 10) + (locationMatch ? 25 : 5) + ratingScore + completionScore);
    }

    private Optional<MatchRecommendationResponse> getCachedRecommendation(String cacheKey, int currentFallbackScore) {
        return cacheRepository.findByCacheKey(cacheKey)
                .filter(cache -> cache.getGeneratedAt() != null && cache.getGeneratedAt().isAfter(LocalDateTime.now().minusHours(6)))
                .map(cache -> fromCache(cache, currentFallbackScore));
    }

    private MatchRecommendationResponse cache(String cacheKey, MatchRecommendationResponse recommendation) {
        AiMatchCache cache = cacheRepository.findByCacheKey(cacheKey).orElseGet(AiMatchCache::new);
        cache.setCacheKey(cacheKey);
        cache.setTargetId(recommendation.getTargetId());
        cache.setAiScore(recommendation.getAiScore());
        cache.setFallbackScore(recommendation.getFallbackScore());
        cache.setLabel(recommendation.getLabel());
        cache.setExplanation(recommendation.getExplanation());
        cache.setStrengths(join(recommendation.getStrengths()));
        cache.setRisks(join(recommendation.getRisks()));
        cache.setRecommendedAction(recommendation.getRecommendedAction());
        cache.setSource(recommendation.getSource());
        cache.setGeneratedAt(recommendation.getGeneratedAt());
        cacheRepository.save(cache);
        return recommendation;
    }

    private MatchRecommendationResponse fromCache(AiMatchCache cache, int currentFallbackScore) {
        return new MatchRecommendationResponse(
                cache.getTargetId(),
                null,
                cache.getAiScore(),
                currentFallbackScore,
                cache.getLabel(),
                cache.getExplanation(),
                splitStored(cache.getStrengths()),
                splitStored(cache.getRisks()),
                cache.getRecommendedAction(),
                cache.getGeneratedAt(),
                cache.getSource()
        );
    }

    private MatchRecommendationResponse fallback(Long targetId, int fallbackScore, String action) {
        return new MatchRecommendationResponse(
                targetId,
                null,
                null,
                fallbackScore,
                labelForScore(fallbackScore),
                "Local AI is unavailable, using built-in match score.",
                List.of(fallbackScore >= 60 ? "Profile and shift details show a reasonable fit" : "Some profile details may still need review"),
                List.of("No AI explanation was generated for this match"),
                action,
                LocalDateTime.now(),
                "FALLBACK"
        );
    }

    private String workerShiftPrompt(User worker, Shift shift) {
        return """
                Rank this shift for the worker.
                Worker: name=%s; skills=%s; location=%s; availability=%s; rating=%.1f; completedShifts=%d.
                Shift: title=%s; role=%s; date=%s; time=%s-%s; location=%s; pay=%s; manager=%s.
                Local fallback score=%d.
                """.formatted(
                safe(worker.getName()),
                safe(worker.getSkills()),
                safe(worker.getLocation()),
                safe(worker.getAvailability()),
                worker.getRating() == null ? 0d : worker.getRating(),
                worker.getCompletedShiftsCount() == null ? 0 : worker.getCompletedShiftsCount(),
                safe(shift.getTitle()),
                safe(shift.getRoleNeeded()),
                safe(shift.getDate()),
                safe(shift.getStartTime()),
                safe(shift.getEndTime()),
                safe(shift.getLocation()),
                shift.getPay() == null ? "unknown" : shift.getPay().toString(),
                safe(shift.getManager() == null ? null : firstPresent(shift.getManager().getRestaurantName(), shift.getManager().getName())),
                scoreWorkerShift(worker, shift)
        );
    }

    private String applicantPrompt(Shift shift, Application application) {
        User worker = application.getWorker();
        return """
                Rank this applicant for the manager's shift.
                Applicant: name=%s; skills=%s; location=%s; availability=%s; rating=%.1f; completedShifts=%d; applicationStatus=%s.
                Shift: title=%s; role=%s; date=%s; time=%s-%s; location=%s; pay=%s.
                Local fallback score=%d.
                """.formatted(
                safe(worker.getName()),
                safe(worker.getSkills()),
                safe(worker.getLocation()),
                safe(worker.getAvailability()),
                worker.getRating() == null ? 0d : worker.getRating(),
                worker.getCompletedShiftsCount() == null ? 0 : worker.getCompletedShiftsCount(),
                safe(application.getStatus()),
                safe(shift.getTitle()),
                safe(shift.getRoleNeeded()),
                safe(shift.getDate()),
                safe(shift.getStartTime()),
                safe(shift.getEndTime()),
                safe(shift.getLocation()),
                shift.getPay() == null ? "unknown" : shift.getPay().toString(),
                scoreWorkerShift(worker, shift)
        );
    }

    private Comparator<MatchRecommendationResponse> scoreComparator() {
        return Comparator
                .comparing((MatchRecommendationResponse recommendation) ->
                        recommendation.getAiScore() == null ? recommendation.getFallbackScore() : recommendation.getAiScore(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MatchRecommendationResponse::getTargetId, Comparator.nullsLast(Long::compareTo));
    }

    private List<MatchRecommendationResponse> rank(List<MatchRecommendationResponse> recommendations) {
        for (int index = 0; index < recommendations.size(); index++) {
            recommendations.get(index).setRank(index + 1);
        }
        return recommendations;
    }

    private List<String> splitTerms(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.toLowerCase().split(",")).stream()
                .map(String::trim)
                .filter(term -> !term.isBlank())
                .toList();
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "not provided" : value.trim();
    }

    private String firstPresent(String first, String second) {
        return first == null || first.isBlank() ? second : first;
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

    private String join(List<String> values) {
        return values == null ? "" : String.join("\n", values);
    }

    private List<String> splitStored(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.split("\n")).stream()
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private boolean isAdmin(User user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    private boolean isManager(User user) {
        return user != null && "MANAGER".equalsIgnoreCase(user.getRole());
    }

    private boolean isWorker(User user) {
        return user != null && "WORKER".equalsIgnoreCase(user.getRole());
    }
}
