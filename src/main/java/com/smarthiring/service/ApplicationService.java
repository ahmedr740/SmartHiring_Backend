package com.smarthiring.service;

import com.smarthiring.dto.ApplyRequest;
import com.smarthiring.dto.RatingRequest;
import com.smarthiring.model.Application;
import com.smarthiring.model.Shift;
import com.smarthiring.model.User;
import com.smarthiring.repository.ApplicationRepository;
import com.smarthiring.repository.ShiftRepository;
import com.smarthiring.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ApplicationService {
    // We kept most application rules here because approval, assignment, and rating
    // all started overlapping once the full demo flow was working.

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final ShiftRepository shiftRepository;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            UserRepository userRepository,
            ShiftRepository shiftRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.shiftRepository = shiftRepository;
    }

    public Application apply(ApplyRequest request, User currentUser) {
        if (request.getShiftId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "shiftId is required");
        }

        User worker = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Worker not found"));
        Shift shift = shiftRepository.findById(request.getShiftId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Shift not found"));

        if (!isWorker(worker)) {
            throw new ResponseStatusException(BAD_REQUEST, "Only workers can apply to shifts");
        }

        if (worker.getStatus() != null && !"ACTIVE".equalsIgnoreCase(worker.getStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "Only active workers can apply to shifts");
        }

        if ("CANCELLED".equalsIgnoreCase(shift.getStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "You cannot apply to a cancelled shift");
        }

        if (!"OPEN".equalsIgnoreCase(shift.getStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "This shift is no longer open for new applications");
        }

        boolean exists = applicationRepository
                .findByWorkerIdAndShiftId(worker.getId(), shift.getId())
                .isPresent();

        if (exists) {
            throw new ResponseStatusException(BAD_REQUEST, "You already applied to this shift");
        }

        Application application = new Application();
        application.setWorker(worker);
        application.setShift(shift);
        application.setStatus("PENDING");
        return applicationRepository.save(application);
    }

    public List<Application> getApplications(User currentUser, Long shiftId) {
        if (shiftId != null) {
            List<Application> applications = applicationRepository.findAllByShiftId(shiftId);

            if (isAdmin(currentUser)) {
                return applications;
            }

            if (isManager(currentUser)) {
                return applications.stream()
                        .filter(application -> application.getShift() != null
                                && application.getShift().getManager() != null
                                && application.getShift().getManager().getId().equals(currentUser.getId()))
                        .toList();
            }

            return applications.stream()
                    .filter(application -> application.getWorker() != null
                            && application.getWorker().getId().equals(currentUser.getId()))
                    .toList();
        }

        if (isAdmin(currentUser)) {
            return applicationRepository.findAll();
        }

        if (isManager(currentUser)) {
            return applicationRepository.findAllByShiftManagerId(currentUser.getId());
        }

        return applicationRepository.findAllByWorkerId(currentUser.getId());
    }

    public Application updateStatus(Long applicationId, String status, User currentUser) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Application not found"));

        boolean isAdmin = isAdmin(currentUser);
        boolean isOwningManager = isOwningManager(currentUser, app.getShift());

        if (!isAdmin && !isOwningManager) {
            throw new ResponseStatusException(FORBIDDEN, "You do not have permission to update this application");
        }

        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Status is required");
        }

        String normalizedStatus = status.trim().toUpperCase();
        if (!List.of("PENDING", "ACCEPTED", "REJECTED").contains(normalizedStatus)) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid status");
        }

        Shift shift = app.getShift();
        if (shift == null) {
            throw new ResponseStatusException(BAD_REQUEST, "This application is not linked to a shift");
        }

        app.setStatus(normalizedStatus);

        if ("ACCEPTED".equals(normalizedStatus)) {
            if (!"OPEN".equalsIgnoreCase(shift.getStatus())
                    && !(shift.getAssignedWorker() != null && shift.getAssignedWorker().getId().equals(app.getWorker().getId()))) {
                throw new ResponseStatusException(BAD_REQUEST, "This shift is not open for assignment");
            }

            shift.setAssignedWorker(app.getWorker());
            shift.setStatus("FILLED");
            shiftRepository.save(shift);

            List<Application> shiftApplications = applicationRepository.findAllByShiftId(shift.getId());
            for (Application shiftApplication : shiftApplications) {
                if (!shiftApplication.getId().equals(app.getId()) && !"REJECTED".equalsIgnoreCase(shiftApplication.getStatus())) {
                    shiftApplication.setStatus("REJECTED");
                    applicationRepository.save(shiftApplication);
                }
            }
        }

        if ("REJECTED".equals(normalizedStatus)
                && shift.getAssignedWorker() != null
                && app.getWorker() != null
                && shift.getAssignedWorker().getId().equals(app.getWorker().getId())) {
            if (List.of("IN_PROGRESS", "COMPLETED").contains((shift.getStatus() == null ? "" : shift.getStatus().toUpperCase()))) {
                throw new ResponseStatusException(BAD_REQUEST, "You cannot remove the accepted worker after the shift has started");
            }

            shift.setAssignedWorker(null);
            shift.setStatus("OPEN");
            shift.setPaid(false);
            shift.setPaidAt(null);
            shiftRepository.save(shift);
        }

        return applicationRepository.save(app);
    }

    public Application submitRating(Long applicationId, RatingRequest request, User currentUser) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Application not found"));

        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new ResponseStatusException(BAD_REQUEST, "Rating must be between 1 and 5");
        }

        if (application.getShift() == null || !"COMPLETED".equalsIgnoreCase(application.getShift().getStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "Ratings are only available after a shift is completed");
        }

        if (!"ACCEPTED".equalsIgnoreCase(application.getStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "Only accepted applications can be rated");
        }

        boolean isWorker = isWorker(currentUser)
                && application.getWorker() != null
                && application.getWorker().getId().equals(currentUser.getId());

        boolean isOwningManager = isOwningManager(currentUser, application.getShift());

        if (!isWorker && !isOwningManager) {
            throw new ResponseStatusException(FORBIDDEN, "You do not have permission to rate this shift");
        }

        if (isWorker) {
            if (application.getManagerRating() != null) {
                throw new ResponseStatusException(BAD_REQUEST, "You have already rated this manager");
            }

            application.setManagerRating(request.getRating());
            application.setManagerReview(cleanReview(request.getReview()));
            application.setManagerRatedAt(LocalDateTime.now());
        } else {
            if (application.getWorkerRating() != null) {
                throw new ResponseStatusException(BAD_REQUEST, "You have already rated this worker");
            }

            application.setWorkerRating(request.getRating());
            application.setWorkerReview(cleanReview(request.getReview()));
            application.setWorkerRatedAt(LocalDateTime.now());
        }

        Application savedApplication = applicationRepository.save(application);
        refreshUserRating(isWorker ? savedApplication.getShift().getManager() : savedApplication.getWorker(), isWorker);
        return savedApplication;
    }

    private String cleanReview(String review) {
        if (review == null) {
            return null;
        }

        String trimmed = review.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void refreshUserRating(User user, boolean ratingManager) {
        if (user == null) {
            return;
        }

        List<Application> acceptedApplications = ratingManager
                ? applicationRepository.findAllByShiftManagerIdAndStatusIgnoreCase(user.getId(), "ACCEPTED")
                : applicationRepository.findAllByWorkerIdAndStatusIgnoreCase(user.getId(), "ACCEPTED");

        List<Integer> ratings = acceptedApplications.stream()
                .map(application -> ratingManager ? application.getManagerRating() : application.getWorkerRating())
                .filter(value -> value != null)
                .toList();

        double averageRating = ratings.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0d);

        user.setRating(averageRating);
        user.setRatingCount(ratings.size());
        userRepository.save(user);
    }

    private boolean isAdmin(User user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    private boolean isManager(User user) {
        return user != null && "MANAGER".equalsIgnoreCase(user.getRole());
    }

    private boolean isWorker(User user) {
        return user != null && "WORKER".equalsIgnoreCase(user.getRole());
    }

    private boolean isOwningManager(User user, Shift shift) {
        return isManager(user)
                && shift != null
                && shift.getManager() != null
                && shift.getManager().getId().equals(user.getId());
    }
}
