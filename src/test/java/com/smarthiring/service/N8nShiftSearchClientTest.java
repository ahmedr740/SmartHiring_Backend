package com.smarthiring.service;

import com.smarthiring.dto.ShiftSearchResponse;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class N8nShiftSearchClientTest {

    @Test
    void returnsSearchResultsFromWebhookResponse() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mockResponse(200, """
                {
                  "interpretation": "Waiter shifts Friday night",
                  "matches": [{"shiftId": 1, "reason": "Matches role and timing"}],
                  "source": "N8N_DEEPSEEK"
                }
                """);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        N8nShiftSearchClient client = new N8nShiftSearchClient(
                true,
                "http://localhost:5678/webhook/staffmatch/shift-search",
                "test-secret",
                "N8N_DEEPSEEK",
                httpClient
        );

        Optional<ShiftSearchResponse> result = client.searchShifts("system", "waiter shifts Friday night", Set.of(1L, 2L));

        assertThat(result).isPresent();
        assertThat(result.get().getSource()).isEqualTo("N8N_DEEPSEEK");
        assertThat(result.get().getMatches()).hasSize(1);
        verify(httpClient).send(
                org.mockito.ArgumentMatchers.argThat(request ->
                        request.uri().toString().endsWith("/shift-search") &&
                                request.headers().firstValue("X-StaffMatch-Webhook-Secret").orElse("").equals("test-secret")
                ),
                any(HttpResponse.BodyHandler.class)
        );
    }

    @Test
    void fallsBackWhenDisabled() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        N8nShiftSearchClient client = new N8nShiftSearchClient(
                false,
                "http://localhost:5678/webhook/staffmatch/shift-search",
                "",
                "N8N_OLLAMA",
                httpClient
        );

        Optional<ShiftSearchResponse> result = client.searchShifts("system", "prompt", Set.of(1L));

        assertThat(result).isEmpty();
        verify(httpClient, never()).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void fallsBackOnWebhookError() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mockResponse(500, "{\"error\":\"n8n failed\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        N8nShiftSearchClient client = new N8nShiftSearchClient(
                true,
                "http://localhost:5678/webhook/staffmatch/shift-search",
                "test-secret",
                "N8N_OLLAMA",
                httpClient
        );

        Optional<ShiftSearchResponse> result = client.searchShifts("system", "prompt", Set.of(1L));

        assertThat(result).isEmpty();
    }

    private HttpResponse<String> mockResponse(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }
}
