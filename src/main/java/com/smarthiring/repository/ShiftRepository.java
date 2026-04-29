package com.smarthiring.repository;

import com.smarthiring.model.Shift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShiftRepository extends JpaRepository<Shift, Long> {
    List<Shift> findAllByManagerId(Long managerId);
    long countByStatusIgnoreCase(String status);
    long countByAssignedWorkerIdAndStatusIgnoreCase(Long workerId, String status);
    long countByManagerIdAndStatusIgnoreCase(Long managerId, String status);
}
