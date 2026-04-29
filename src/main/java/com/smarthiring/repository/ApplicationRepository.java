package com.smarthiring.repository;

import com.smarthiring.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// START of interface
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByWorkerIdAndShiftId(Long workerId, Long shiftId);

    List<Application> findAllByWorkerId(Long workerId);

    List<Application> findAllByShiftId(Long shiftId);

    List<Application> findAllByShiftManagerId(Long managerId);

    List<Application> findAllByWorkerIdAndStatusIgnoreCase(Long workerId, String status);

    List<Application> findAllByShiftManagerIdAndStatusIgnoreCase(Long managerId, String status);

    long countByStatusIgnoreCase(String status);

    void deleteAllByShiftId(Long shiftId);
}
