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

class OllamaShiftSearchClientTest {

    @Test
    void returnsSearchResultsFromGenerateResponse() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mockResponse(200, """
                {
                  "model": "llama3.2:3b",
                  "response": "{\\"interpretation\\":\\"Waiter shifts Friday night\\",\\"matches\\":[{\\"shiftId\\":1,\\"reason\\":\\"Matches role and timing\\"}]}",
                  "done": true
                }
                """);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        OllamaShiftSearchClient client = new OllamaShiftSearchClient(
                true,
                "http://localhost:11434",
                "llama3.2:3b",
                httpClient,
                new com.fasterxml.jackson.databind.ObjectMapper(),
                new ShiftSearchJsonParser()
        );

        Optional<ShiftSearchResponse> result = client.searchShifts("system", "waiter shifts Friday night", Set.of(1L, 2L));

        assertThat(result).isPresent();
        assertThat(result.get().getSource()).isEqualTo("OLLAMA");
        assertThat(result.get().getMatches()).hasSize(1);
        assertThat(result.get().getMatches().get(0).getShiftId()).isEqualTo(1L);
        verify(httpClient).send(
                org.mockito.ArgumentMatchers.argThat(request -> request.uri().toString().endsWith("/api/generate")),
                any(HttpResponse.BodyHandler.class)
        );
    }

    @Test
    void fallsBackWhenDisabled() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        OllamaShiftSearchClient client = new OllamaShiftSearchClient(
                false,
                "http://localhost:11434",
                "llama3.2:3b",
                httpClient,
                new com.fasterxml.jackson.databind.ObjectMapper(),
                new ShiftSearchJsonParser()
        );

        Optional<ShiftSearchResponse> result = client.searchShifts("system", "prompt", Set.of(1L));

        assertThat(result).isEmpty();
        verify(httpClient, never()).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void fallsBackOnOllamaError() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mockResponse(500, "{\"error\":\"model not found\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        OllamaShiftSearchClient client = new OllamaShiftSearchClient(
                true,
                "http://localhost:11434",
                "llama3.2:3b",
                httpClient,
                new com.fasterxml.jackson.databind.ObjectMapper(),
                new ShiftSearchJsonParser()
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
