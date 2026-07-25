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

class OllamaShiftDraftClientTest {

    @Test
    void returnsDraftFromGenerateResponse() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mockResponse(200, """
                {
                  "model": "llama3.2:3b",
                  "response": "{\\"title\\":\\"Friday Night Server\\",\\"description\\":\\"Busy weekend service.\\",\\"requirements\\":\\"Available Friday evening\\",\\"roleNeeded\\":\\"WAITER\\",\\"pay\\":18,\\"date\\":\\"2026-07-24\\",\\"startTime\\":\\"18:00\\",\\"endTime\\":\\"23:00\\",\\"location\\":\\"\\",\\"assumptions\\":\\"Assumed the upcoming Friday.\\"}",
                  "done": true
                }
                """);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        OllamaShiftDraftClient client = new OllamaShiftDraftClient(
                true,
                "http://localhost:11434",
                "llama3.2:3b",
                httpClient,
                new com.fasterxml.jackson.databind.ObjectMapper(),
                new ShiftDraftJsonParser()
        );

        Optional<ShiftDraftResponse> draft = client.draftShift("system", "need 2 servers Fri night, $18/hr");

        assertThat(draft).isPresent();
        assertThat(draft.get().getSource()).isEqualTo("OLLAMA");
        assertThat(draft.get().getTitle()).isEqualTo("Friday Night Server");
        assertThat(draft.get().getRoleNeeded()).isEqualTo("WAITER");
        assertThat(draft.get().getPay()).isEqualTo(18.0);
        verify(httpClient).send(
                org.mockito.ArgumentMatchers.argThat(request -> request.uri().toString().endsWith("/api/generate")),
                any(HttpResponse.BodyHandler.class)
        );
    }

    @Test
    void fallsBackWhenDisabled() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        OllamaShiftDraftClient client = new OllamaShiftDraftClient(
                false,
                "http://localhost:11434",
                "llama3.2:3b",
                httpClient,
                new com.fasterxml.jackson.databind.ObjectMapper(),
                new ShiftDraftJsonParser()
        );

        Optional<ShiftDraftResponse> draft = client.draftShift("system", "prompt");

        assertThat(draft).isEmpty();
        verify(httpClient, never()).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void fallsBackOnOllamaError() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mockResponse(500, "{\"error\":\"model not found\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);

        OllamaShiftDraftClient client = new OllamaShiftDraftClient(
                true,
                "http://localhost:11434",
                "llama3.2:3b",
                httpClient,
                new com.fasterxml.jackson.databind.ObjectMapper(),
                new ShiftDraftJsonParser()
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
