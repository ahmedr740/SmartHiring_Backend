package com.smarthiring.service;

import com.smarthiring.dto.ShiftDraftResponse;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class N8nShiftDraftClientTest {

    @Test
    void returnsDraftFromWebhookResponse() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mockResponse(200, """
                {
                  "title": "Friday Night Server",
                  "description": "Two servers needed for a busy Friday night shift.",
                  "requirements": "Comfortable with high-volume weekend service",
                  "roleNeeded": "WAITER",
                  "pay": 18,
                  "date": "2026-07-24",
                  "startTime": "18:00",
                  "endTime": "23:00",
                  "location": "",
                  "assumptions": "Assumed the upcoming Friday.",
                  "source": "N8N_DEEPSEEK"
                }
                """);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        N8nShiftDraftClient client = new N8nShiftDraftClient(
                true,
                "http://localhost:5678/webhook/staffmatch/shift-draft",
                "test-secret",
                "N8N_DEEPSEEK",
                httpClient
        );

        Optional<ShiftDraftResponse> draft = client.draftShift("system", "need 2 servers Fri night, $18/hr");

        assertThat(draft).isPresent();
        assertThat(draft.get().getSource()).isEqualTo("N8N_DEEPSEEK");
        assertThat(draft.get().getTitle()).isEqualTo("Friday Night Server");
        assertThat(draft.get().getRoleNeeded()).isEqualTo("WAITER");
        verify(httpClient).send(
                org.mockito.ArgumentMatchers.argThat(request ->
                        request.uri().toString().endsWith("/shift-draft") &&
                                request.headers().firstValue("X-StaffMatch-Webhook-Secret").orElse("").equals("test-secret")
                ),
                any(HttpResponse.BodyHandler.class)
        );
    }

    @Test
    void fallsBackWhenDisabled() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        N8nShiftDraftClient client = new N8nShiftDraftClient(
                false,
                "http://localhost:5678/webhook/staffmatch/shift-draft",
                "",
                "N8N_DEEPSEEK",
                httpClient
        );

        Optional<ShiftDraftResponse> draft = client.draftShift("system", "prompt");

        assertThat(draft).isEmpty();
        verify(httpClient, never()).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void fallsBackOnWebhookError() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mockResponse(500, "{\"error\":\"n8n failed\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        N8nShiftDraftClient client = new N8nShiftDraftClient(
                true,
                "http://localhost:5678/webhook/staffmatch/shift-draft",
                "test-secret",
                "N8N_DEEPSEEK",
                httpClient
        );

        Optional<ShiftDraftResponse> draft = client.draftShift("system", "prompt");

        assertThat(draft).isEmpty();
    }

    private HttpResponse<String> mockResponse(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }
}
