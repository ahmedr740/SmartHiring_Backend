package com.smarthiring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthiring.dto.ShiftSearchResponse;
import com.smarthiring.model.Shift;
import com.smarthiring.model.User;
import com.smarthiring.repository.ShiftRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
public class ShiftSearchService {

    private static final int MAX_QUERY_LENGTH = 500;
    private static final int MAX_CANDIDATE_SHIFTS = 40;

    private static final String SYSTEM_PROMPT = """
            You help a restaurant worker search open shifts on the HubPin marketplace using a plain-language description.
            You are given the worker's request and a list of currently open shifts, each with an id, title, role, date, time, location, and pay.
            Select the shifts that best fit the worker's stated date, time of day, role, location, or pay preferences.
            Prefer close or similar matches over an empty result when nothing matches exactly, but exclude shifts that clearly conflict with an explicit preference.
            Only use shiftId values from the supplied list; never invent one.
            Order matches from most to least relevant and return at most 12.
            Briefly summarize what you understood from the request in "interpretation".
            """;

    private final ExternalShiftSearchClient externalShiftSearchClient;
    private final ShiftRepository shiftRepository;
    private final ObjectMapper objectMapper;

    public ShiftSearchService(ExternalShiftSearchClient externalShiftSearchClient, ShiftRepository shiftRepository) {
        this.externalShiftSearchClient = externalShiftSearchClient;
        this.shiftRepository = shiftRepository;
        this.objectMapper = new ObjectMapper();
    }

    public ShiftSearchResponse searchShifts(String query, User worker) {
        if (!isWorker(worker)) {
            throw new ResponseStatusException(FORBIDDEN, "Only workers can use the shift search assistant");
        }

        String trimmedQuery = query == null ? "" : query.trim();
        if (trimmedQuery.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Describe the kind of shift you're looking for first");
        }
        if (trimmedQuery.length() > MAX_QUERY_LENGTH) {
            throw new ResponseStatusException(BAD_REQUEST, "Keep your search under " + MAX_QUERY_LENGTH + " characters");
        }

        List<Shift> openShifts = shiftRepository.findAllByStatusIgnoreCase("OPEN").stream()
                .limit(MAX_CANDIDATE_SHIFTS)
                .toList();

        if (openShifts.isEmpty()) {
            ShiftSearchResponse empty = new ShiftSearchResponse();
            empty.setInterpretation("There are no open shifts to search right now.");
            empty.setMatches(List.of());
            empty.setGeneratedAt(LocalDateTime.now());
            empty.setSource("NONE");
            return empty;
        }

        Set<Long> allowedShiftIds = openShifts.stream().map(Shift::getId).collect(Collectors.toSet());

        return externalShiftSearchClient
                .searchShifts(SYSTEM_PROMPT, buildUserPrompt(trimmedQuery, openShifts), allowedShiftIds)
                .orElseThrow(() -> new ResponseStatusException(
                        SERVICE_UNAVAILABLE,
                        "AI shift search is unavailable right now. Try the search box above instead."
                ));
    }

    private String buildUserPrompt(String query, List<Shift> openShifts) {
        try {
            String shiftsJson = objectMapper.writeValueAsString(openShifts.stream().map(this::toCandidate).toList());
            return """
                    Worker's request: "%s"

                    Open shifts (JSON):
                    %s

                    Return JSON only with this shape:
                    {"interpretation": "...", "matches": [{"shiftId": number, "reason": "..."}]}
                    """.formatted(query, shiftsJson);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize open shifts for shift search", e);
        }
    }

    private Map<String, Object> toCandidate(Shift shift) {
        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("shiftId", shift.getId());
        candidate.put("title", safe(shift.getTitle()));
        candidate.put("role", safe(shift.getRoleNeeded()));
        candidate.put("date", safe(shift.getDate()));
        candidate.put("startTime", safe(shift.getStartTime()));
        candidate.put("endTime", safe(shift.getEndTime()));
        candidate.put("location", safe(shift.getLocation()));
        candidate.put("pay", shift.getPay());
        return candidate;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "not provided" : value.trim();
    }

    private boolean isWorker(User user) {
        return user != null && "WORKER".equalsIgnoreCase(user.getRole());
    }
}
