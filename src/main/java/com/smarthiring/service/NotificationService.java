package com.smarthiring.service;

import com.smarthiring.dto.NotificationResponse;
import com.smarthiring.model.Notification;
import com.smarthiring.model.User;
import com.smarthiring.repository.NotificationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class NotificationService {

    public static final int MAX_EMAIL_ATTEMPTS = 3;

    private final NotificationRepository notificationRepository;
    private final N8nNotificationClient n8nClient;

    public NotificationService(NotificationRepository notificationRepository, N8nNotificationClient n8nClient) {
        this.notificationRepository = notificationRepository;
        this.n8nClient = n8nClient;
    }

    @Transactional
    public Notification create(
            User recipient,
            String type,
            String title,
            String message,
            String actionUrl,
            boolean emailEligible,
            String dedupeKey
    ) {
        if (recipient == null || recipient.getId() == null || dedupeKey == null || dedupeKey.isBlank()) {
            return null;
        }

        return notificationRepository.findByDedupeKey(dedupeKey).orElseGet(() -> {
            Notification notification = new Notification();
            notification.setRecipient(recipient);
            notification.setType(type);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setActionUrl(actionUrl);
            notification.setEmailEligible(emailEligible);
            notification.setEmailStatus(emailEligible ? "PENDING" : "NOT_APPLICABLE");
            notification.setEmailAttempts(0);
            notification.setDedupeKey(dedupeKey);
            try {
                return notificationRepository.saveAndFlush(notification);
            } catch (DataIntegrityViolationException duplicate) {
                return notificationRepository.findByDedupeKey(dedupeKey).orElse(null);
            }
        });
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(User user, int requestedLimit, boolean unreadOnly) {
        int limit = Math.max(1, Math.min(requestedLimit, 100));
        List<Notification> notifications = unreadOnly
                ? notificationRepository.findAllByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, limit))
                : notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, limit));
        return notifications.stream().map(NotificationResponse::from).toList();
    }

    public long unreadCount(User user) {
        return notificationRepository.countByRecipientIdAndReadAtIsNull(user.getId());
    }

    @Transactional
    public NotificationResponse markRead(User user, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Notification not found"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
        return NotificationResponse.from(notification);
    }

    @Transactional
    public void markAllRead(User user) {
        List<Notification> unread = notificationRepository
                .findAllByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, 1000));
        LocalDateTime now = LocalDateTime.now();
        unread.forEach(notification -> notification.setReadAt(now));
        notificationRepository.saveAll(unread);
    }

    @Scheduled(
            initialDelayString = "${n8n.notification-dispatch-initial-delay-ms:10000}",
            fixedDelayString = "${n8n.notification-dispatch-delay-ms:30000}"
    )
    @Transactional
    public void dispatchPendingEmails() {
        if (!n8nClient.isEnabled()) {
            return;
        }

        List<Notification> pending = notificationRepository
                .findByEmailEligibleTrueAndEmailStatusInAndEmailAttemptsLessThanOrderByCreatedAtAsc(
                        List.of("PENDING", "RETRY"),
                        MAX_EMAIL_ATTEMPTS,
                        PageRequest.of(0, 20)
                );

        for (Notification notification : pending) {
            int attempts = notification.getEmailAttempts() == null ? 0 : notification.getEmailAttempts();
            attempts++;
            notification.setEmailAttempts(attempts);
            notification.setLastEmailAttemptAt(LocalDateTime.now());
            boolean sent = n8nClient.send(notification);
            notification.setEmailStatus(sent ? "SENT" : attempts >= MAX_EMAIL_ATTEMPTS ? "FAILED" : "RETRY");
            notificationRepository.save(notification);
        }
    }
}
