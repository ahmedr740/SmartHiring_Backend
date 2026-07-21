package com.smarthiring.service;

import com.smarthiring.dto.MockPaymentRequest;
import com.smarthiring.model.MockPayment;
import com.smarthiring.model.Shift;
import com.smarthiring.model.User;
import com.smarthiring.repository.MockPaymentRepository;
import com.smarthiring.repository.ShiftRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class MockPaymentService {

    private final MockPaymentRepository mockPaymentRepository;
    private final ShiftRepository shiftRepository;
    private final NotificationService notificationService;

    public MockPaymentService(MockPaymentRepository mockPaymentRepository, ShiftRepository shiftRepository) {
        this(mockPaymentRepository, shiftRepository, null);
    }

    @Autowired
    public MockPaymentService(
            MockPaymentRepository mockPaymentRepository,
            ShiftRepository shiftRepository,
            NotificationService notificationService
    ) {
        this.mockPaymentRepository = mockPaymentRepository;
        this.shiftRepository = shiftRepository;
        this.notificationService = notificationService;
    }

    public List<MockPayment> getPayments(User currentUser) {
        if ("ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            return mockPaymentRepository.findAllByOrderByCreatedAtDesc();
        }

        if ("MANAGER".equalsIgnoreCase(currentUser.getRole())) {
            return mockPaymentRepository.findAllByManagerIdOrderByCreatedAtDesc(currentUser.getId());
        }

        return mockPaymentRepository.findAllByWorkerIdOrderByCreatedAtDesc(currentUser.getId());
    }

    public MockPayment startPayment(Long shiftId, User currentUser) {
        Shift shift = findPayableShift(shiftId, currentUser);
        return mockPaymentRepository.findByShiftId(shift.getId())
                .orElseGet(() -> {
                    MockPayment payment = new MockPayment();
                    payment.setShift(shift);
                    payment.setWorker(shift.getAssignedWorker());
                    payment.setManager(shift.getManager());
                    payment.setAmount(calculateAmount(shift));
                    payment.setStatus(Boolean.TRUE.equals(shift.getPaid()) ? "PAID" : "PENDING");
                    payment.setMethodLabel("Card");
                    if (Boolean.TRUE.equals(shift.getPaid())) {
                        payment.setPaidAt(shift.getPaidAt());
                    }
                    return mockPaymentRepository.save(payment);
                });
    }

    @Transactional
    public MockPayment confirmPayment(Long paymentId, MockPaymentRequest request, User currentUser) {
        MockPayment payment = mockPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Payment not found"));
        Shift shift = findPayableShift(payment.getShift().getId(), currentUser);

        payment.setShift(shift);
        payment.setWorker(shift.getAssignedWorker());
        payment.setManager(shift.getManager());
        payment.setAmount(calculateAmount(shift));
        payment.setMethodLabel(cleanMethodLabel(request == null ? null : request.getMethodLabel()));
        payment.setStatus("PAID");
        payment.setPaidAt(LocalDateTime.now());

        shift.setPaid(true);
        shift.setPaidAt(payment.getPaidAt());
        shiftRepository.save(shift);

        MockPayment saved = mockPaymentRepository.save(payment);
        if (notificationService != null && saved.getWorker() != null) {
            notificationService.create(
                    saved.getWorker(),
                    "PAYMENT_COMPLETED",
                    "Payment completed",
                    "Payment for %s has been completed.".formatted(shift.getTitle()),
                    "/worker-jobs",
                    true,
                    "payment-completed:%d".formatted(saved.getId())
            );
        }
        return saved;
    }

    @Transactional
    public MockPayment startAndConfirm(Long shiftId, User currentUser) {
        MockPayment payment = startPayment(shiftId, currentUser);
        MockPaymentRequest request = new MockPaymentRequest();
        request.setMethodLabel("Card");
        return confirmPayment(payment.getId(), request, currentUser);
    }

    private Shift findPayableShift(Long shiftId, User currentUser) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Shift not found"));

        boolean canPay = "ADMIN".equalsIgnoreCase(currentUser.getRole())
                || ("MANAGER".equalsIgnoreCase(currentUser.getRole())
                && shift.getManager() != null
                && shift.getManager().getId().equals(currentUser.getId()));

        if (!canPay) {
            throw new ResponseStatusException(FORBIDDEN, "You do not have permission to pay this shift");
        }

        if (!"COMPLETED".equalsIgnoreCase(shift.getStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "Only completed shifts can be paid");
        }

        if (shift.getAssignedWorker() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "A worker must be assigned before payment");
        }

        return shift;
    }

    private Double calculateAmount(Shift shift) {
        double hours = 1d;
        try {
            LocalTime start = LocalTime.parse(shift.getStartTime());
            LocalTime end = LocalTime.parse(shift.getEndTime());
            long minutes = ChronoUnit.MINUTES.between(start, end);
            if (minutes > 0) {
                hours = minutes / 60d;
            }
        } catch (RuntimeException ignored) {
            hours = 1d;
        }

        return Math.round((shift.getPay() == null ? 0d : shift.getPay()) * hours * 100d) / 100d;
    }

    private String cleanMethodLabel(String methodLabel) {
        if (methodLabel == null || methodLabel.isBlank()) {
            return "Card";
        }

        return methodLabel.trim();
    }
}
