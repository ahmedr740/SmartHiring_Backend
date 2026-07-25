package com.smarthiring.service;

import com.smarthiring.dto.ShiftDraftResponse;
import com.smarthiring.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
public class ShiftDraftService {

    private static final int MAX_INPUT_LENGTH = 2000;

    private static final String SYSTEM_PROMPT = """
            You turn a restaurant manager's rough shift notes into a structured shift posting for the HubPin marketplace.
            Only use information present in the manager's notes and the supplied reference date.
            Never invent a specific street address, business name, or exact pay rate that was not stated or clearly implied.
            Resolve relative dates such as "Friday", "this weekend", or "tomorrow" into a concrete date using the reference date, and note that assumption.
            Leave a field as an empty string (or null for pay) if it cannot be reasonably inferred from the notes.
            roleNeeded must be exactly one of: WAITER, CHEF, BARISTA, CASHIER, KITCHEN HELPER, or an empty string if none clearly fit.
            Write practical, concise copy suitable for a shift marketplace listing. Do not mention protected traits.
            """;

    private final ExternalShiftDraftClient externalShiftDraftClient;
    private final Clock clock;

    @Autowired
    public ShiftDraftService(ExternalShiftDraftClient externalShiftDraftClient) {
        this(externalShiftDraftClient, Clock.systemDefaultZone());
    }

    ShiftDraftService(ExternalShiftDraftClient externalShiftDraftClient, Clock clock) {
        this.externalShiftDraftClient = externalShiftDraftClient;
        this.clock = clock;
    }

    public ShiftDraftResponse draftShift(String managerInput, User manager) {
        if (!isManager(manager)) {
            throw new ResponseStatusException(FORBIDDEN, "Only managers can use the shift-posting assistant");
        }
        if (!"ACTIVE".equalsIgnoreCase(manager.getStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "This manager account is not approved to post shifts");
        }

        String trimmedInput = managerInput == null ? "" : managerInput.trim();
        if (trimmedInput.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Describe the shift you want to post first");
        }
        if (trimmedInput.length() > MAX_INPUT_LENGTH) {
            throw new ResponseStatusException(BAD_REQUEST, "Keep your shift notes under " + MAX_INPUT_LENGTH + " characters");
        }

        return externalShiftDraftClient.draftShift(SYSTEM_PROMPT, buildUserPrompt(trimmedInput))
                .orElseThrow(() -> new ResponseStatusException(
                        SERVICE_UNAVAILABLE,
                        "AI shift drafting is unavailable right now. Please fill in the form manually."
                ));
    }

    private String buildUserPrompt(String managerInput) {
        LocalDate today = LocalDate.now(clock);
        return """
                Reference date: %s (%s).
                Manager's rough shift notes:
                %s

                Return JSON only with this shape:
                {"title": "...", "description": "...", "requirements": "...", "roleNeeded": "...", "pay": number or null, "date": "YYYY-MM-DD or empty", "startTime": "HH:mm or empty", "endTime": "HH:mm or empty", "location": "...", "assumptions": "..."}
                """.formatted(today, today.getDayOfWeek(), managerInput);
    }

    private boolean isManager(User user) {
        return user != null && "MANAGER".equalsIgnoreCase(user.getRole());
    }
}
