package com.smarthiring.service;

import com.smarthiring.dto.NotificationResponse;
import com.smarthiring.model.Notification;
import com.smarthiring.model.User;
import com.smarthiring.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final N8nNotificationClient client = mock(N8nNotificationClient.class);
    private final NotificationService service = new NotificationService(repository, client);

    @Test
    void createsOneNotificationPerDedupeKey() {
        User worker = user(1L);
        when(repository.findByDedupeKey("ai:1:10")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(99L);
            return notification;
        });

        Notification created = service.create(
                worker,
                "AI_JOB_MATCH",
                "Strong match",
                "A shift scored 90%.",
                "/worker-matches",
                true,
                "ai:1:10"
        );

        assertThat(created.getRecipient()).isEqualTo(worker);
        assertThat(created.getEmailStatus()).isEqualTo("PENDING");
        verify(repository).saveAndFlush(any(Notification.class));

        when(repository.findByDedupeKey("ai:1:10")).thenReturn(Optional.of(created));
        Notification duplicate = service.create(worker, "AI_JOB_MATCH", "Changed", "Changed", "/", true, "ai:1:10");
        assertThat(duplicate).isSameAs(created);
        verify(repository, times(1)).saveAndFlush(any(Notification.class));
    }

    @Test
    void listsOnlyTheAuthenticatedUsersNotifications() {
        User worker = user(7L);
        Notification notification = notification(worker, 20L);
        when(repository.findAllByRecipientIdOrderByCreatedAtDesc(eq(7L), any(Pageable.class)))
                .thenReturn(List.of(notification));

        List<NotificationResponse> response = service.list(worker, 20, false);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).id()).isEqualTo(20L);
        verify(repository).findAllByRecipientIdOrderByCreatedAtDesc(eq(7L), any(Pageable.class));
    }

    @Test
    void rejectsReadingAnotherUsersNotification() {
        User worker = user(7L);
        when(repository.findByIdAndRecipientId(20L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(worker, 20L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void retriesFailedEmailWithoutDeletingInAppNotification() {
        User worker = user(1L);
        worker.setEmail("worker@example.com");
        Notification pending = notification(worker, 10L);
        pending.setEmailEligible(true);
        pending.setEmailStatus("PENDING");
        pending.setEmailAttempts(0);

        when(client.isEnabled()).thenReturn(true);
        when(client.send(pending)).thenReturn(false);
        when(repository.findByEmailEligibleTrueAndEmailStatusInAndEmailAttemptsLessThanOrderByCreatedAtAsc(
                anyList(), eq(3), any(Pageable.class)
        )).thenReturn(List.of(pending));

        service.dispatchPendingEmails();

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getEmailAttempts()).isEqualTo(1);
        assertThat(saved.getValue().getEmailStatus()).isEqualTo("RETRY");
        assertThat(saved.getValue().getRecipient()).isEqualTo(worker);
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("Worker");
        return user;
    }

    private Notification notification(User recipient, Long id) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setRecipient(recipient);
        notification.setType("APPLICATION_ACCEPTED");
        notification.setTitle("Accepted");
        notification.setMessage("Your application was accepted.");
        notification.setDedupeKey("notification:" + id);
        return notification;
    }
}
