package com.smarthiring.controller;

import com.smarthiring.model.LikedJob;
import com.smarthiring.model.User;
import com.smarthiring.repository.UserRepository;
import com.smarthiring.service.LikedJobService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/liked-jobs")
public class LikedJobController {

    private final LikedJobService likedJobService;
    private final UserRepository userRepository;

    public LikedJobController(LikedJobService likedJobService, UserRepository userRepository) {
        this.likedJobService = likedJobService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<LikedJob> getLikedJobs(Authentication authentication) {
        return likedJobService.getLikedJobs(currentUser(authentication));
    }

    @PostMapping("/{shiftId}")
    public LikedJob likeShift(@PathVariable Long shiftId, Authentication authentication) {
        return likedJobService.likeShift(shiftId, currentUser(authentication));
    }

    @DeleteMapping("/{shiftId}")
    public void unlikeShift(@PathVariable Long shiftId, Authentication authentication) {
        likedJobService.unlikeShift(shiftId, currentUser(authentication));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
