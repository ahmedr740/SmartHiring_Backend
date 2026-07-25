package com.smarthiring.controller;

import com.smarthiring.dto.MatchRecommendationResponse;
import com.smarthiring.dto.ShiftSearchRequest;
import com.smarthiring.dto.ShiftSearchResponse;
import com.smarthiring.model.User;
import com.smarthiring.repository.UserRepository;
import com.smarthiring.service.MatchingService;
import com.smarthiring.service.ShiftSearchService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchingController {

    private final MatchingService matchingService;
    private final UserRepository userRepository;
    private final ShiftSearchService shiftSearchService;

    public MatchingController(
            MatchingService matchingService,
            UserRepository userRepository,
            ShiftSearchService shiftSearchService
    ) {
        this.matchingService = matchingService;
        this.userRepository = userRepository;
        this.shiftSearchService = shiftSearchService;
    }

    @GetMapping("/worker/shifts")
    public List<MatchRecommendationResponse> getWorkerShiftMatches(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        return matchingService.getWorkerShiftMatches(currentUser);
    }

    @GetMapping("/manager/shifts/{shiftId}/applicants")
    public List<MatchRecommendationResponse> getManagerApplicantMatches(
            @PathVariable Long shiftId,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        return matchingService.getManagerApplicantMatches(shiftId, currentUser);
    }

    @PostMapping("/worker/shifts/search")
    public ShiftSearchResponse searchWorkerShifts(@RequestBody ShiftSearchRequest request, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        return shiftSearchService.searchShifts(request.getQuery(), currentUser);
    }

    private User getCurrentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
