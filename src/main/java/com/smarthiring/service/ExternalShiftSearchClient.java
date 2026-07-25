package com.smarthiring.service;

import com.smarthiring.dto.ShiftSearchResponse;

import java.util.Optional;
import java.util.Set;

public interface ExternalShiftSearchClient {
    Optional<ShiftSearchResponse> searchShifts(String systemPrompt, String userPrompt, Set<Long> allowedShiftIds);
}
