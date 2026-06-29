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

class OllamaMatchingClientTest {

    @Test
    void returnsOllamaRecommendationFromGenerateResponse() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mockResponse(200, """
                {
                  "model": "llama3.2:3b",
                  "response": "{\\"targetId\\":10,\\"aiScore\\":88,\\"label\\":\\"Strong match\\",\\"explanation\\":\\"Strong skill fit.\\",\\"strengths\\":[\\"Waiter skills\\"],\\"risks\\":[\\"Confirm availability\\"],\\"recommendedAction\\":\\"Apply now.\\"}",
                  "done": true
                }
                """);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        OllamaMatchingClient client = new OllamaMatchingClient(
                true,
                "http://localhost:11434",
                "llama3.2:3b",
                httpClient,
                new com.fasterxml.jackson.databind.ObjectMapper(),
                new MatchRecommendationJsonParser()
        );

        Optional<MatchRecommendationResponse> recommendation = client.scoreMatch(
                "system",
                "Worker details here.",
                10L,
                75,
                MatchType.WORKER_SHIFT
        );

        assertThat(recommendation).isPresent();
        assertThat(recommendation.get().getSource()).isEqualTo("OLLAMA");
        assertThat(recommendation.get().getAiScore()).isEqualTo(88);
        assertThat(recommendation.get().getStrengths()).contains("Waiter skills");
        verify(httpClient).send(
                org.mockito.ArgumentMatchers.argThat(request ->
                        request.uri().toString().endsWith("/api/generate")
                ),
                any(HttpResponse.BodyHandler.class)
        );
    }

    @Test
    void fallsBackWhenDisabled() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        OllamaMatchingClient client = new OllamaMatchingClient(
                false,
                "http://localhost:11434",
                "llama3.2:3b",
                httpClient,
                new com.fasterxml.jackson.databind.ObjectMapper(),
                new MatchRecommendationJsonParser()
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
    void fallsBackOnOllamaError() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mockResponse(500, "{\"error\":\"model not found\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        OllamaMatchingClient client = new OllamaMatchingClient(
                true,
                "http://localhost:11434",
                "llama3.2:3b",
                httpClient,
                new com.fasterxml.jackson.databind.ObjectMapper(),
                new MatchRecommendationJsonParser()
        );

        Optional<MatchRecommendationResponse> recommendation = client.scoreMatch(
                "system",
                "prompt",
                10L,
                75,
                MatchType.MANAGER_APPLICANT
        );

        assertThat(recommendation).isEmpty();
    }

    private HttpResponse<String> mockResponse(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }
}
