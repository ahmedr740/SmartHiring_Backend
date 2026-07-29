package com.smarthiring.config;

import com.smarthiring.service.ExternalMatchingClient;
import com.smarthiring.service.N8nMatchingClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MatchingClientConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.matching.enabled", havingValue = "true", matchIfMissing = true)
    ExternalMatchingClient n8nMatchingClient(
            @Value("${n8n.worker-match-webhook-url:http://localhost:5678/webhook/staffmatch/worker-shift-match}") String workerMatchWebhookUrl,
            @Value("${n8n.manager-match-webhook-url:http://localhost:5678/webhook/staffmatch/manager-applicant-match}") String managerMatchWebhookUrl,
            @Value("${n8n.webhook-secret:}") String webhookSecret
    ) {
        return new N8nMatchingClient(
                true,
                workerMatchWebhookUrl,
                managerMatchWebhookUrl,
                webhookSecret,
                "N8N_DEEPSEEK"
        );
    }

    @Bean
    @ConditionalOnProperty(name = "app.matching.enabled", havingValue = "false")
    ExternalMatchingClient disabledMatchingClient() {
        return (systemPrompt, userPrompt, targetId, fallbackScore, matchType) -> java.util.Optional.empty();
    }
}
