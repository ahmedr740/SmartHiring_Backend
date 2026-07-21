package com.smarthiring.controller;

import com.smarthiring.dto.IssueReportRequest;
import com.smarthiring.model.IssueReport;
import com.smarthiring.model.User;
import com.smarthiring.repository.UserRepository;
import com.smarthiring.service.IssueReportService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/issues")
public class IssueReportController {

    private final IssueReportService issueReportService;
    private final UserRepository userRepository;

    public IssueReportController(IssueReportService issueReportService, UserRepository userRepository) {
        this.issueReportService = issueReportService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<IssueReport> getIssues(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        return issueReportService.getIssues(currentUser);
    }

    @PostMapping
    public IssueReport createIssue(@RequestBody IssueReportRequest request, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        return issueReportService.createIssue(request, currentUser);
    }

    @PutMapping("/{id}/status")
    public IssueReport updateStatus(
            @PathVariable Long id,
            @RequestBody IssueReportRequest request,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        return issueReportService.updateStatus(id, request.getStatus(), currentUser);
    }

    private User getCurrentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
