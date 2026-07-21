package com.smarthiring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthiring.model.Notification;
import com.smarthiring.model.User;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class N8nNotificationClientTest {

    @Test
    void sendsSecretAndSanitizedNotificationPayload() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        N8nNotificationClient client = new N8nNotificationClient(
                true,
                "http://localhost:5678/webhook/smart-hiring/notification-email",
                "test-secret",
                "http://localhost:3000",
                httpClient,
                new ObjectMapper().findAndRegisterModules()
        );

        boolean sent = client.send(notification());

        assertThat(sent).isTrue();
        verify(httpClient).send(
                argThat(request ->
                        request.headers().firstValue("X-StaffMatch-Webhook-Secret").orElse("").equals("test-secret")
                                && request.uri().toString().endsWith("/notification-email")
                ),
                any(HttpResponse.BodyHandler.class)
        );
    }

    @Test
    void disabledClientDoesNotCallWebhook() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        N8nNotificationClient client = new N8nNotificationClient(
                false,
                "http://localhost:5678/webhook/smart-hiring/notification-email",
                "test-secret",
                "http://localhost:3000",
                httpClient,
                new ObjectMapper().findAndRegisterModules()
        );

        assertThat(client.send(notification())).isFalse();
        verify(httpClient, never()).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    private Notification notification() {
        User worker = new User();
        worker.setId(1L);
        worker.setName("Worker");
        worker.setEmail("worker@example.com");
        Notification notification = new Notification();
        notification.setId(10L);
        notification.setRecipient(worker);
        notification.setType("APPLICATION_ACCEPTED");
        notification.setTitle("Application accepted");
        notification.setMessage("Your application was accepted.");
        notification.setActionUrl("/worker-jobs");
        notification.setCreatedAt(LocalDateTime.now());
        return notification;
    }
}
