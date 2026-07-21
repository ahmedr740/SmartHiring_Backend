package com.smarthiring.service;

import com.smarthiring.dto.IssueReportRequest;
import com.smarthiring.model.Application;
import com.smarthiring.model.IssueReport;
import com.smarthiring.model.Shift;
import com.smarthiring.model.User;
import com.smarthiring.repository.ApplicationRepository;
import com.smarthiring.repository.IssueReportRepository;
import com.smarthiring.repository.ShiftRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class IssueReportService {

    private final IssueReportRepository issueReportRepository;
    private final ApplicationRepository applicationRepository;
    private final ShiftRepository shiftRepository;
    private final NotificationService notificationService;

    public IssueReportService(
            IssueReportRepository issueReportRepository,
            ApplicationRepository applicationRepository,
            ShiftRepository shiftRepository,
            NotificationService notificationService
    ) {
        this.issueReportRepository = issueReportRepository;
        this.applicationRepository = applicationRepository;
        this.shiftRepository = shiftRepository;
        this.notificationService = notificationService;
    }

    public List<IssueReport> getIssues(User currentUser) {
        if (isAdmin(currentUser)) {
            return issueReportRepository.findAllByOrderByCreatedAtDesc();
        }

        if (isManager(currentUser)) {
            return issueReportRepository.findAllByShiftManagerIdOrderByCreatedAtDesc(currentUser.getId());
        }

        return issueReportRepository.findAllByReportedByIdOrderByCreatedAtDesc(currentUser.getId());
    }

    public IssueReport createIssue(IssueReportRequest request, User currentUser) {
        if (request.getDescription() == null || request.getDescription().trim().length() < 10) {
            throw new ResponseStatusException(BAD_REQUEST, "Please describe the issue in at least 10 characters");
        }

        Application application = null;
        Shift shift = null;

        if (request.getApplicationId() != null) {
            application = applicationRepository.findById(request.getApplicationId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Application not found"));
            shift = application.getShift();
        }

        if (request.getShiftId() != null) {
            shift = shiftRepository.findById(request.getShiftId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Shift not found"));
        }

        if (application == null && shift == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Select a shift or application to report");
        }

        if (!canReport(currentUser, application, shift)) {
            throw new ResponseStatusException(FORBIDDEN, "You do not have permission to report this item");
        }

        IssueReport issue = new IssueReport();
        issue.setReportedBy(currentUser);
        issue.setApplication(application);
        issue.setShift(shift);
        issue.setCategory(cleanCategory(request.getCategory()));
        issue.setDescription(request.getDescription().trim());
        issue.setStatus("OPEN");
        return issueReportRepository.save(issue);
    }

    public IssueReport updateStatus(Long issueId, String status, User currentUser) {
        if (!isAdmin(currentUser)) {
            throw new ResponseStatusException(FORBIDDEN, "Only admins can update issue status");
        }

        IssueReport issue = issueReportRepository.findById(issueId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Issue report not found"));
        issue.setStatus(normalizeStatus(status));
        IssueReport saved = issueReportRepository.save(issue);
        if (saved.getReportedBy() != null) {
            notificationService.create(
                    saved.getReportedBy(),
                    "ISSUE_STATUS_CHANGED",
                    "Issue status updated",
                    "Your reported issue is now %s.".formatted(saved.getStatus().toLowerCase()),
                    isManager(saved.getReportedBy()) ? "/manager-home" : "/worker-jobs",
                    true,
                    "issue-status:%d:%s".formatted(saved.getId(), saved.getStatus())
            );
        }
        return saved;
    }

    private boolean canReport(User user, Application application, Shift shift) {
        if (isAdmin(user)) {
            return true;
        }

        if (application != null && application.getWorker() != null && application.getWorker().getId().equals(user.getId())) {
            return true;
        }

        return isManager(user)
                && shift != null
                && shift.getManager() != null
                && shift.getManager().getId().equals(user.getId());
    }

    private String cleanCategory(String category) {
        if (category == null || category.isBlank()) {
            return "GENERAL";
        }

        String normalized = category.trim().toUpperCase();
        if (!List.of("GENERAL", "PAYMENT", "NO_SHOW", "SAFETY", "QUALITY", "OTHER").contains(normalized)) {
            return "OTHER";
        }

        return normalized;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Status is required");
        }

        String normalized = status.trim().toUpperCase();
        if (!List.of("OPEN", "REVIEWING", "RESOLVED").contains(normalized)) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid issue status");
        }

        return normalized;
    }

    private boolean isAdmin(User user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    private boolean isManager(User user) {
        return user != null && "MANAGER".equalsIgnoreCase(user.getRole());
    }
}
