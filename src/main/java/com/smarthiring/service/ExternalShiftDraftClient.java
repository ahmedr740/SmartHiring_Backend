package com.smarthiring.service;

import com.smarthiring.dto.ShiftDraftResponse;

import java.util.Optional;

public interface ExternalShiftDraftClient {
    Optional<ShiftDraftResponse> draftShift(String systemPrompt, String userPrompt);
}
