package com.smarthiring.service;

import com.smarthiring.dto.MatchRecommendationResponse;
import com.smarthiring.dto.MatchType;
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

class N8nMatchingClientTest {

    @Test
    void returnsN8nOllamaRecommendationFromWebhookResponse() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mockResponse(200, """
                {
                  "targetId": 10,
                  "aiScore": 88,
                  "fallbackScore": 75,
                  "label": "Strong match",
                  "explanation": "Local Ollama found strong skill and location alignment.",
                  "strengths": ["Waiter skills", "Central location"],
                  "risks": ["Confirm availability"],
                  "recommendedAction": "Invite the worker to apply.",
                  "source": "N8N_OLLAMA"
                }
                """);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        N8nMatchingClient client = new N8nMatchingClient(
                true,
                "http://localhost:5678/webhook/staffmatch/worker-shift-match",
                "http://localhost:5678/webhook/staffmatch/manager-applicant-match",
                "test-secret",
                httpClient
        );

        Optional<MatchRecommendationResponse> recommendation = client.scoreMatch(
                "system",
                "Rank this shift for the worker.",
                10L,
                75,
                MatchType.WORKER_SHIFT
        );

        assertThat(recommendation).isPresent();
        assertThat(recommendation.get().getSource()).isEqualTo("N8N_OLLAMA");
        assertThat(recommendation.get().getAiScore()).isEqualTo(88);
        assertThat(recommendation.get().getStrengths()).contains("Waiter skills");
        verify(httpClient).send(
                org.mockito.ArgumentMatchers.argThat(request ->
                        request.uri().toString().endsWith("/worker-shift-match") &&
                                request.headers().firstValue("X-StaffMatch-Webhook-Secret").orElse("").equals("test-secret")
                ),
                any(HttpResponse.BodyHandler.class)
        );
    }

    @Test
    void fallsBackWhenDisabled() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        N8nMatchingClient client = new N8nMatchingClient(
                false,
                "http://localhost:5678/webhook/staffmatch/worker-shift-match",
                "http://localhost:5678/webhook/staffmatch/manager-applicant-match",
                "",
                httpClient
        );

        Optional<MatchRecommendationResponse> recommendation = client.scoreMatch(
                "system",
                "prompt",
                10L,
                75,
                MatchType.WORKER_SHIFT
        );

        assertThat(recommendation).isEmpty();
        verify(httpClient, never()).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void fallsBackOnWebhookError() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mockResponse(500, "{\"error\":\"n8n failed\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);
        N8nMatchingClient client = new N8nMatchingClient(
                true,
                "http://localhost:5678/webhook/staffmatch/worker-shift-match",
                "http://localhost:5678/webhook/staffmatch/manager-applicant-match",
                "test-secret",
                httpClient
        );

        Optional<MatchRecommendationResponse> recommendation = client.scoreMatch(
                "system",
                "prompt",
                10L,
                75,
                MatchType.WORKER_SHIFT
        );

        assertThat(recommendation).isEmpty();
    }

    @Test
    void managerMatchTypeUsesManagerWebhook() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mockResponse(200, "{\"targetId\":10,\"aiScore\":80}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);
        N8nMatchingClient client = new N8nMatchingClient(
                true,
                "http://localhost:5678/webhook/staffmatch/worker-shift-match",
                "http://localhost:5678/webhook/staffmatch/manager-applicant-match",
                "",
                httpClient
        );

        Optional<MatchRecommendationResponse> recommendation = client.scoreMatch(
                "system",
                "Rank this applicant for the shift.",
                10L,
                75,
                MatchType.MANAGER_APPLICANT
        );

        assertThat(recommendation).isPresent();
        verify(httpClient).send(
                org.mockito.ArgumentMatchers.argThat(request ->
                        request.uri().toString().endsWith("/manager-applicant-match")
                ),
                any(HttpResponse.BodyHandler.class)
        );
    }

    private HttpResponse<String> mockResponse(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }
}
