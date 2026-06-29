package com.smarthiring.repository;

import com.smarthiring.model.LikedJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LikedJobRepository extends JpaRepository<LikedJob, Long> {
    List<LikedJob> findAllByWorkerIdOrderByCreatedAtDesc(Long workerId);
    Optional<LikedJob> findByWorkerIdAndShiftId(Long workerId, Long shiftId);
    boolean existsByWorkerIdAndShiftId(Long workerId, Long shiftId);
    void deleteByWorkerIdAndShiftId(Long workerId, Long shiftId);
}
