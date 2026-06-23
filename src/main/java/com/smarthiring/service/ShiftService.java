package com.smarthiring.service;

import com.smarthiring.model.Shift;
import com.smarthiring.model.User;
import com.smarthiring.repository.ApplicationRepository;
import com.smarthiring.repository.ShiftRepository;
import com.smarthiring.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ShiftService {
    // We kept shift lifecycle logic in one service so it was easier for the team to debug
    // status-related issues during testing and demos.

    private final ShiftRepository shiftRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    public ShiftService(
            ShiftRepository shiftRepository,
            ApplicationRepository applicationRepository,
            UserRepository userRepository
    ) {
        this.shiftRepository = shiftRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
    }

    public Shift createShift(Shift shift) {
        shift.setAssignedWorker(null);
        shift.setPaid(false);
        shift.setPaidAt(null);
        shift.setCompletedAt(null);
        shift.setStatus("OPEN");
        return shiftRepository.save(shift);
    }

    public List<Shift> getAllShifts() {
        return shiftRepository.findAll();
    }

    public List<Shift> getVisibleShifts(User currentUser) {
        if ("ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            return shiftRepository.findAll();
        }

        if ("MANAGER".equalsIgnoreCase(currentUser.getRole())) {
            return shiftRepository.findAllByManagerId(currentUser.getId());
        }

        return shiftRepository.findAll().stream()
                .filter(shift -> {
                    String status = shift.getStatus() == null ? "OPEN" : shift.getStatus();
                    boolean isVisibleOpenShift = "OPEN".equalsIgnoreCase(status);
                    boolean isAssignedToWorker =
                            shift.getAssignedWorker() != null
                                    && shift.getAssignedWorker().getId().equals(currentUser.getId())
                                    && List.of("FILLED", "IN_PROGRESS", "COMPLETED").contains(status.toUpperCase());
                    return isVisibleOpenShift || isAssignedToWorker;
                })
                .toList();
    }

    public Shift updateStatus(Long shiftId, String status, User currentUser) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Shift not found"));

        boolean canModerate =
                "ADMIN".equalsIgnoreCase(currentUser.getRole()) ||
                        ("MANAGER".equalsIgnoreCase(currentUser.getRole()) && shift.getManager() != null
                                && shift.getManager().getId().equals(currentUser.getId()));

        if (!canModerate) {
            throw new ResponseStatusException(FORBIDDEN, "You do not have permission to update this shift");
        }

        String normalizedStatus = status == null ? "" : status.trim().toUpperCase();
        if (!List.of("OPEN", "FILLED", "IN_PROGRESS", "COMPLETED", "CANCELLED").contains(normalizedStatus)) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid shift status");
        }

        validateLifecycleTransition(shift, normalizedStatus);
        shift.setStatus(normalizedStatus);

        if ("COMPLETED".equals(normalizedStatus)) {
            shift.setCompletedAt(LocalDateTime.now());
        } else if (!"COMPLETED".equals(normalizedStatus)) {
            shift.setCompletedAt(null);
        }

        if ("CANCELLED".equals(normalizedStatus)) {
            shift.setAssignedWorker(null);
            shift.setPaid(false);
            shift.setPaidAt(null);
        }

        Shift savedShift = shiftRepository.save(shift);

        if ("COMPLETED".equals(normalizedStatus)) {
            refreshCompletionStats(savedShift);
        }

        return savedShift;
    }

    public Shift markPaid(Long shiftId, User currentUser) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Shift not found"));

        boolean canModerate =
                "ADMIN".equalsIgnoreCase(currentUser.getRole()) ||
                        ("MANAGER".equalsIgnoreCase(currentUser.getRole()) && shift.getManager() != null
                                && shift.getManager().getId().equals(currentUser.getId()));

        if (!canModerate) {
            throw new ResponseStatusException(FORBIDDEN, "You do not have permission to update this shift");
        }

        if (!"COMPLETED".equalsIgnoreCase(shift.getStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "Only completed shifts can be marked as paid");
        }

        shift.setPaid(true);
        shift.setPaidAt(LocalDateTime.now());
        return shiftRepository.save(shift);
    }

    public Shift updateShift(Long shiftId, Shift request, User currentUser) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Shift not found"));

        boolean canEdit =
                "ADMIN".equalsIgnoreCase(currentUser.getRole()) ||
                        ("MANAGER".equalsIgnoreCase(currentUser.getRole()) && shift.getManager() != null
                                && shift.getManager().getId().equals(currentUser.getId()));

        if (!canEdit) {
            throw new ResponseStatusException(FORBIDDEN, "You do not have permission to edit this shift");
        }

        String status = shift.getStatus() == null ? "OPEN" : shift.getStatus().trim().toUpperCase();
        if (List.of("IN_PROGRESS", "COMPLETED").contains(status)) {
            throw new ResponseStatusException(BAD_REQUEST, "Shifts cannot be edited after they have started");
        }

        if (request.getTitle() != null) {
            shift.setTitle(request.getTitle().trim());
        }
        if (request.getDate() != null) {
            shift.setDate(request.getDate().trim());
        }
        if (request.getStartTime() != null) {
            shift.setStartTime(request.getStartTime().trim());
        }
        if (request.getEndTime() != null) {
            shift.setEndTime(request.getEndTime().trim());
        }
        if (request.getPay() != null) {
            if (request.getPay() <= 0) {
                throw new ResponseStatusException(BAD_REQUEST, "Pay must be greater than zero");
            }
            shift.setPay(request.getPay());
        }
        if (request.getRoleNeeded() != null) {
            shift.setRoleNeeded(request.getRoleNeeded().trim());
        }
        if (request.getLocation() != null) {
            shift.setLocation(request.getLocation().trim());
        }

        validateShiftFields(shift);
        return shiftRepository.save(shift);
    }

    @Transactional
    public void deleteShift(Long shiftId, User currentUser) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Shift not found"));

        boolean canDelete =
                "ADMIN".equalsIgnoreCase(currentUser.getRole()) ||
                        ("MANAGER".equalsIgnoreCase(currentUser.getRole()) && shift.getManager() != null
                                && shift.getManager().getId().equals(currentUser.getId()));

        if (!canDelete) {
            throw new ResponseStatusException(FORBIDDEN, "You do not have permission to delete this shift");
        }

        applicationRepository.deleteAllByShiftId(shiftId);
        shiftRepository.delete(shift);
    }

    private void validateLifecycleTransition(Shift shift, String nextStatus) {
        String currentStatus = shift.getStatus() == null ? "OPEN" : shift.getStatus().trim().toUpperCase();

        if ("COMPLETED".equals(currentStatus) && !"COMPLETED".equals(nextStatus)) {
            throw new ResponseStatusException(BAD_REQUEST, "Completed shifts cannot move back to another status");
        }

        if ("OPEN".equals(nextStatus) && shift.getAssignedWorker() != null) {
            throw new ResponseStatusException(BAD_REQUEST, "Use application moderation to reopen an assigned shift");
        }

        if ("FILLED".equals(nextStatus) && shift.getAssignedWorker() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "A worker must be assigned before a shift can be filled");
        }

        if ("IN_PROGRESS".equals(nextStatus)) {
            if (shift.getAssignedWorker() == null || !"FILLED".equals(currentStatus)) {
                throw new ResponseStatusException(BAD_REQUEST, "Only filled shifts can move to in progress");
            }
        }

        if ("COMPLETED".equals(nextStatus)) {
            if (shift.getAssignedWorker() == null || !List.of("FILLED", "IN_PROGRESS").contains(currentStatus)) {
                throw new ResponseStatusException(BAD_REQUEST, "Only staffed shifts can be completed");
            }
        }
    }

    private void refreshCompletionStats(Shift shift) {
        if (shift.getAssignedWorker() != null) {
            User worker = shift.getAssignedWorker();
            worker.setCompletedShiftsCount((int) shiftRepository.countByAssignedWorkerIdAndStatusIgnoreCase(worker.getId(), "COMPLETED"));
            userRepository.save(worker);
        }

        if (shift.getManager() != null) {
            User manager = shift.getManager();
            manager.setCompletedShiftsCount((int) shiftRepository.countByManagerIdAndStatusIgnoreCase(manager.getId(), "COMPLETED"));
            userRepository.save(manager);
        }
    }

    private void validateShiftFields(Shift shift) {
        if (isBlank(shift.getTitle()) || isBlank(shift.getDate()) || isBlank(shift.getStartTime())
                || isBlank(shift.getEndTime()) || isBlank(shift.getRoleNeeded()) || isBlank(shift.getLocation())) {
            throw new ResponseStatusException(BAD_REQUEST, "Title, date, time, role, and location are required");
        }

        if (shift.getPay() == null || shift.getPay() <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Pay must be greater than zero");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
