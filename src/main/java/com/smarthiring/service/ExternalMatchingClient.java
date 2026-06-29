package com.smarthiring.service;

import com.smarthiring.dto.MatchRecommendationResponse;
import com.smarthiring.dto.MatchType;

import java.util.Optional;

public interface ExternalMatchingClient {
    Optional<MatchRecommendationResponse> scoreMatch(
            String systemPrompt,
            String userPrompt,
            Long targetId,
            int fallbackScore,
            MatchType matchType
    );
}
