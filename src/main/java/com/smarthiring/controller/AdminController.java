package com.smarthiring.controller;

import com.smarthiring.model.User;
import com.smarthiring.model.Shift;
import com.smarthiring.model.Application;
import com.smarthiring.repository.ApplicationRepository;
import com.smarthiring.repository.IssueReportRepository;
import com.smarthiring.repository.ShiftRepository;
import com.smarthiring.repository.UserRepository;
import com.smarthiring.service.NotificationService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final ShiftRepository shiftRepository;
    private final ApplicationRepository applicationRepository;
    private final IssueReportRepository issueReportRepository;
    private final NotificationService notificationService;

    public AdminController(
            UserRepository userRepository,
            ShiftRepository shiftRepository,
            ApplicationRepository applicationRepository,
            IssueReportRepository issueReportRepository,
            NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.shiftRepository = shiftRepository;
        this.applicationRepository = applicationRepository;
        this.issueReportRepository = issueReportRepository;
        this.notificationService = notificationService;
    }

    @GetMapping("/overview")
    public Map<String, Object> getOverview() {
        Map<String, Object> response = new LinkedHashMap<>();
        List<User> users = userRepository.findAll();
        List<Shift> shifts = shiftRepository.findAll();
        List<Application> applications = applicationRepository.findAll();

        response.put("totalUsers", userRepository.count());
        response.put("totalManagers", userRepository.countByRoleIgnoreCase("MANAGER"));
        response.put("activeManagers", userRepository.countByRoleIgnoreCaseAndStatusIgnoreCase("MANAGER", "ACTIVE"));
        response.put("totalWorkers", userRepository.countByRoleIgnoreCase("WORKER"));
        response.put("pendingManagers", userRepository.countByRoleIgnoreCaseAndStatusIgnoreCase("MANAGER", "PENDING"));
        response.put("openShifts", shiftRepository.countByStatusIgnoreCase("OPEN"));
        response.put("filledShifts", shiftRepository.countByStatusIgnoreCase("FILLED"));
        response.put("cancelledShifts", shiftRepository.countByStatusIgnoreCase("CANCELLED"));
        response.put("totalApplications", applicationRepository.count());
        response.put("pendingApplications", applicationRepository.countByStatusIgnoreCase("PENDING"));
        response.put("acceptedApplications", applicationRepository.countByStatusIgnoreCase("ACCEPTED"));
        response.put("rejectedApplications", applicationRepository.countByStatusIgnoreCase("REJECTED"));
        response.put("openIssues", issueReportRepository.countByStatusIgnoreCase("OPEN"));
        response.put("reviewingIssues", issueReportRepository.countByStatusIgnoreCase("REVIEWING"));
        response.put("resolvedIssues", issueReportRepository.countByStatusIgnoreCase("RESOLVED"));
        response.put("completedShifts", shifts.stream().filter(shift -> "COMPLETED".equalsIgnoreCase(shift.getStatus())).count());
        response.put(
                "paidCompletedShifts",
                shifts.stream().filter(shift -> "COMPLETED".equalsIgnoreCase(shift.getStatus()) && Boolean.TRUE.equals(shift.getPaid())).count()
        );
        response.put(
                "unpaidCompletedShifts",
                shifts.stream().filter(shift -> "COMPLETED".equalsIgnoreCase(shift.getStatus()) && !Boolean.TRUE.equals(shift.getPaid())).count()
        );
        response.put(
                "averageWorkerRating",
                users.stream()
                        .filter(user -> "WORKER".equalsIgnoreCase(user.getRole()) && user.getRatingCount() != null && user.getRatingCount() > 0)
                        .mapToDouble(user -> user.getRating() == null ? 0d : user.getRating())
                        .average()
                        .orElse(0d)
        );
        response.put(
                "averageManagerRating",
                users.stream()
                        .filter(user -> "MANAGER".equalsIgnoreCase(user.getRole()) && user.getRatingCount() != null && user.getRatingCount() > 0)
                        .mapToDouble(user -> user.getRating() == null ? 0d : user.getRating())
                        .average()
                        .orElse(0d)
        );
        response.put(
                "recentUsers",
                users.stream()
                        .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(6)
                        .toList()
        );
        response.put(
                "recentCompletedShifts",
                shifts.stream()
                        .filter(shift -> "COMPLETED".equalsIgnoreCase(shift.getStatus()))
                        .sorted(Comparator.comparing(Shift::getCompletedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(5)
                        .toList()
        );
        response.put(
                "topManagers",
                users.stream()
                        .filter(user -> "MANAGER".equalsIgnoreCase(user.getRole()))
                        .sorted(
                                Comparator.comparing(User::getRating, Comparator.nullsFirst(Comparator.reverseOrder()))
                                        .thenComparing(User::getCompletedShiftsCount, Comparator.nullsFirst(Comparator.reverseOrder()))
                        )
                        .limit(5)
                        .toList()
        );
        response.put(
                "topWorkers",
                users.stream()
                        .filter(user -> "WORKER".equalsIgnoreCase(user.getRole()))
                        .sorted(
                                Comparator.comparing(User::getRating, Comparator.nullsFirst(Comparator.reverseOrder()))
                                        .thenComparing(User::getCompletedShiftsCount, Comparator.nullsFirst(Comparator.reverseOrder()))
                        )
                        .limit(5)
                        .toList()
        );
        response.put(
                "recentRatings",
                applications.stream()
                        .filter(application -> application.getWorkerRating() != null || application.getManagerRating() != null)
                        .sorted(Comparator.comparing(
                                application -> {
                                    if (application.getWorkerRatedAt() == null) {
                                        return application.getManagerRatedAt();
                                    }
                                    if (application.getManagerRatedAt() == null) {
                                        return application.getWorkerRatedAt();
                                    }
                                    return application.getWorkerRatedAt().isAfter(application.getManagerRatedAt())
                                            ? application.getWorkerRatedAt()
                                            : application.getManagerRatedAt();
                                },
                                Comparator.nullsLast(Comparator.reverseOrder())
                        ))
                        .limit(6)
                        .toList()
        );
        return response;
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @GetMapping("/manager-requests")
    public List<User> getPendingManagerRequests() {
        return userRepository.findAllByRoleIgnoreCaseAndStatusIgnoreCase("MANAGER", "PENDING").stream()
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @PutMapping("/users/{id}/status")
    public User updateStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));

        String previousStatus = user.getStatus();
        String status = normalizeStatus(request.get("status"));
        user.setStatus(status);
        User saved = userRepository.save(user);
        if (!status.equalsIgnoreCase(previousStatus == null ? "" : previousStatus)) {
            String destination = "MANAGER".equalsIgnoreCase(saved.getRole()) ? "/manager-home" : "/worker-home";
            notificationService.create(
                    saved,
                    "ACCOUNT_STATUS_CHANGED",
                    accountStatusTitle(status),
                    "Your Smart Hiring account status is now %s.".formatted(status.toLowerCase()),
                    destination,
                    true,
                    "account-status:%d:%s".formatted(saved.getId(), status)
            );
        }
        return saved;
    }

    @PutMapping("/users/{id}/role")
    public User updateRole(@PathVariable Long id, @RequestBody Map<String, String> request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));

        String role = normalizeRole(request.get("role"));
        user.setRole(role);

        if ("MANAGER".equals(role) && (user.getStatus() == null || "ACTIVE".equalsIgnoreCase(user.getStatus()))) {
            user.setStatus("PENDING");
        } else if (!"MANAGER".equals(role)) {
            user.setStatus("ACTIVE");
        }

        return userRepository.save(user);
    }

    @PutMapping("/users/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getSkills() != null) {
            user.setSkills(request.getSkills());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getLocation() != null) {
            user.setLocation(request.getLocation());
        }
        if (request.getAvailability() != null) {
            user.setAvailability(request.getAvailability());
        }
        if (request.getRestaurantName() != null) {
            user.setRestaurantName(request.getRestaurantName());
        }

        return userRepository.save(user);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Status is required");
        }

        String normalizedStatus = status.trim().toUpperCase();
        if (!List.of("PENDING", "ACTIVE", "REJECTED", "SUSPENDED").contains(normalizedStatus)) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid status");
        }

        return normalizedStatus;
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Role is required");
        }

        String normalizedRole = role.trim().toUpperCase();
        if (!List.of("ADMIN", "MANAGER", "WORKER").contains(normalizedRole)) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid role");
        }

        return normalizedRole;
    }

    private String accountStatusTitle(String status) {
        return switch (status) {
            case "ACTIVE" -> "Account approved";
            case "REJECTED" -> "Account rejected";
            case "SUSPENDED" -> "Account suspended";
            default -> "Account status updated";
        };
    }
}
