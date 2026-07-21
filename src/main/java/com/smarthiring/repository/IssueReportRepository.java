package com.smarthiring.repository;

import com.smarthiring.model.IssueReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueReportRepository extends JpaRepository<IssueReport, Long> {
    List<IssueReport> findAllByReportedByIdOrderByCreatedAtDesc(Long reportedById);
    List<IssueReport> findAllByShiftManagerIdOrderByCreatedAtDesc(Long managerId);
    List<IssueReport> findAllByOrderByCreatedAtDesc();
    long countByStatusIgnoreCase(String status);
}
