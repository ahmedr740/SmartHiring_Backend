package com.smarthiring.config;

import com.smarthiring.service.ExternalMatchingClient;
import com.smarthiring.service.N8nMatchingClient;
import com.smarthiring.service.OllamaMatchingClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MatchingClientConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.matching.provider", havingValue = "ollama", matchIfMissing = true)
    ExternalMatchingClient ollamaMatchingClient(
            @Value("${app.matching.enabled:true}") boolean matchingEnabled,
            @Value("${ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${ollama.model:llama3.2:3b}") String model
    ) {
        return new OllamaMatchingClient(matchingEnabled, baseUrl, model);
    }

    @Bean
    @ConditionalOnProperty(name = "app.matching.provider", havingValue = "n8n")
    ExternalMatchingClient n8nMatchingClient(
            @Value("${n8n.matching-enabled:true}") boolean matchingEnabled,
            @Value("${n8n.worker-match-webhook-url:http://localhost:5678/webhook/staffmatch/worker-shift-match}") String workerMatchWebhookUrl,
            @Value("${n8n.manager-match-webhook-url:http://localhost:5678/webhook/staffmatch/manager-applicant-match}") String managerMatchWebhookUrl,
            @Value("${n8n.webhook-secret:}") String webhookSecret
    ) {
        return new N8nMatchingClient(matchingEnabled, workerMatchWebhookUrl, managerMatchWebhookUrl, webhookSecret);
    }

    @Bean
    @ConditionalOnProperty(name = "app.matching.provider", havingValue = "none")
    ExternalMatchingClient disabledMatchingClient() {
        return (systemPrompt, userPrompt, targetId, fallbackScore, matchType) -> java.util.Optional.empty();
    }
}
