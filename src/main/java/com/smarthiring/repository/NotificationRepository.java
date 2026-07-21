package com.smarthiring.repository;

import com.smarthiring.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByDedupeKey(String dedupeKey);
    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);
    List<Notification> findAllByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);
    List<Notification> findAllByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(Long recipientId, Pageable pageable);
    long countByRecipientIdAndReadAtIsNull(Long recipientId);
    List<Notification> findByEmailEligibleTrueAndEmailStatusInAndEmailAttemptsLessThanOrderByCreatedAtAsc(
            List<String> statuses,
            Integer attempts,
            Pageable pageable
    );
}
