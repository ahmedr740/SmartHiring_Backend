package com.smarthiring.config;

import com.smarthiring.service.ExternalShiftSearchClient;
import com.smarthiring.service.N8nShiftSearchClient;
import com.smarthiring.service.OllamaShiftSearchClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ShiftSearchClientConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.matching.provider", havingValue = "ollama", matchIfMissing = true)
    ExternalShiftSearchClient ollamaShiftSearchClient(
            @Value("${app.matching.enabled:true}") boolean enabled,
            @Value("${ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${ollama.model:llama3.2:3b}") String model
    ) {
        return new OllamaShiftSearchClient(enabled, baseUrl, model);
    }

    @Bean
    @ConditionalOnProperty(name = "app.matching.provider", havingValue = "n8n")
    ExternalShiftSearchClient n8nShiftSearchClient(
            @Value("${app.matching.enabled:true}") boolean enabled,
            @Value("${n8n.shift-search-webhook-url:http://localhost:5678/webhook/staffmatch/shift-search}") String webhookUrl,
            @Value("${n8n.webhook-secret:}") String webhookSecret,
            @Value("${n8n.match-source:N8N_OLLAMA}") String matchSource
    ) {
        return new N8nShiftSearchClient(enabled, webhookUrl, webhookSecret, matchSource);
    }

    @Bean
    @ConditionalOnProperty(name = "app.matching.provider", havingValue = "none")
    ExternalShiftSearchClient disabledShiftSearchClient() {
        return (systemPrompt, userPrompt, allowedShiftIds) -> java.util.Optional.empty();
    }
}
