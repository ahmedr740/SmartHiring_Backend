package com.smarthiring.config;

import com.smarthiring.service.ExternalShiftDraftClient;
import com.smarthiring.service.N8nShiftDraftClient;
import com.smarthiring.service.OllamaShiftDraftClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ShiftDraftClientConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.matching.provider", havingValue = "ollama", matchIfMissing = true)
    ExternalShiftDraftClient ollamaShiftDraftClient(
            @Value("${app.matching.enabled:true}") boolean enabled,
            @Value("${ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${ollama.model:llama3.2:3b}") String model
    ) {
        return new OllamaShiftDraftClient(enabled, baseUrl, model);
    }

    @Bean
    @ConditionalOnProperty(name = "app.matching.provider", havingValue = "n8n")
    ExternalShiftDraftClient n8nShiftDraftClient(
            @Value("${app.matching.enabled:true}") boolean enabled,
            @Value("${n8n.shift-draft-webhook-url:http://localhost:5678/webhook/staffmatch/shift-draft}") String webhookUrl,
            @Value("${n8n.webhook-secret:}") String webhookSecret,
            @Value("${n8n.match-source:N8N_OLLAMA}") String matchSource
    ) {
        return new N8nShiftDraftClient(enabled, webhookUrl, webhookSecret, matchSource);
    }

    @Bean
    @ConditionalOnProperty(name = "app.matching.provider", havingValue = "none")
    ExternalShiftDraftClient disabledShiftDraftClient() {
        return (systemPrompt, userPrompt) -> java.util.Optional.empty();
    }
}
