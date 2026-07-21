package com.smarthiring.service;

import com.smarthiring.dto.MockPaymentRequest;
import com.smarthiring.model.MockPayment;
import com.smarthiring.model.Shift;
import com.smarthiring.model.User;
import com.smarthiring.repository.MockPaymentRepository;
import com.smarthiring.repository.ShiftRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class MockPaymentServiceTest {

    private final MockPaymentRepository mockPaymentRepository = mock(MockPaymentRepository.class);
    private final ShiftRepository shiftRepository = mock(ShiftRepository.class);
    private final MockPaymentService mockPaymentService = new MockPaymentService(mockPaymentRepository, shiftRepository);

    @Test
    void startsPaymentForCompletedShiftOwnedByManager() {
        User manager = manager(2L);
        Shift shift = completedShift(manager, worker(1L));

        when(shiftRepository.findById(shift.getId())).thenReturn(Optional.of(shift));
        when(mockPaymentRepository.findByShiftId(shift.getId())).thenReturn(Optional.empty());
        when(mockPaymentRepository.save(any(MockPayment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockPayment payment = mockPaymentService.startPayment(shift.getId(), manager);

        assertThat(payment.getShift()).isEqualTo(shift);
        assertThat(payment.getStatus()).isEqualTo("PENDING");
        assertThat(payment.getAmount()).isEqualTo(100d);
    }

    @Test
    void rejectsPaymentBeforeShiftCompleted() {
        User manager = manager(2L);
        Shift shift = completedShift(manager, worker(1L));
        shift.setStatus("IN_PROGRESS");

        when(shiftRepository.findById(shift.getId())).thenReturn(Optional.of(shift));

        assertThatThrownBy(() -> mockPaymentService.startPayment(shift.getId(), manager))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    @Test
    void confirmPaymentMarksShiftPaid() {
        User manager = manager(2L);
        Shift shift = completedShift(manager, worker(1L));
        MockPayment payment = new MockPayment();
        payment.setId(30L);
        payment.setShift(shift);

        MockPaymentRequest request = new MockPaymentRequest();
        request.setMethodLabel("Demo Card ending 4242");

        when(mockPaymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(shiftRepository.findById(shift.getId())).thenReturn(Optional.of(shift));
        when(mockPaymentRepository.save(any(MockPayment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockPayment saved = mockPaymentService.confirmPayment(payment.getId(), request, manager);

        assertThat(saved.getStatus()).isEqualTo("PAID");
        assertThat(saved.getPaidAt()).isNotNull();
        assertThat(shift.getPaid()).isTrue();
        verify(shiftRepository).save(shift);
    }

    private User worker(Long id) {
        User user = new User();
        user.setId(id);
        user.setRole("WORKER");
        user.setStatus("ACTIVE");
        return user;
    }

    private User manager(Long id) {
        User user = new User();
        user.setId(id);
        user.setRole("MANAGER");
        user.setStatus("ACTIVE");
        return user;
    }

    private Shift completedShift(User manager, User worker) {
        Shift shift = new Shift();
        shift.setId(10L);
        shift.setManager(manager);
        shift.setAssignedWorker(worker);
        shift.setStatus("COMPLETED");
        shift.setPay(20d);
        shift.setStartTime("18:00");
        shift.setEndTime("23:00");
        return shift;
    }
}
