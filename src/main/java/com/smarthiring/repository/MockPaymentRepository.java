package com.smarthiring.repository;

import com.smarthiring.model.MockPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MockPaymentRepository extends JpaRepository<MockPayment, Long> {
    Optional<MockPayment> findByShiftId(Long shiftId);
    List<MockPayment> findAllByWorkerIdOrderByCreatedAtDesc(Long workerId);
    List<MockPayment> findAllByManagerIdOrderByCreatedAtDesc(Long managerId);
    List<MockPayment> findAllByOrderByCreatedAtDesc();
}
