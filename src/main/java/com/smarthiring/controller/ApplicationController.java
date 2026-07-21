package com.smarthiring.controller;

import com.smarthiring.dto.ApplicationStatusUpdateRequest;
import com.smarthiring.dto.ApplyRequest;
import com.smarthiring.dto.RatingRequest;
import com.smarthiring.model.Application;
import com.smarthiring.model.User;
import com.smarthiring.repository.UserRepository;
import com.smarthiring.service.ApplicationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final UserRepository userRepository;

    public ApplicationController(ApplicationService applicationService, UserRepository userRepository) {
        this.applicationService = applicationService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public Application apply(@RequestBody ApplyRequest request, Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return applicationService.apply(request, currentUser);
    }

    @GetMapping
    public List<Application> getApplications(
            @RequestParam(required = false) Long shiftId,
            Authentication authentication
    ) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return applicationService.getApplications(currentUser, shiftId);
    }

    @PutMapping("/{id}/status")
    public Application updateStatus(
            @PathVariable Long id,
            @RequestBody ApplicationStatusUpdateRequest request,
            Authentication authentication
    ) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return applicationService.updateStatus(id, request.getStatus(), currentUser);
    }

    @PutMapping("/{id}/rating")
    public Application submitRating(
            @PathVariable Long id,
            @RequestBody RatingRequest request,
            Authentication authentication
    ) {
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return applicationService.submitRating(id, request, currentUser);
    }
}
