package com.smarthiring.config;

import com.smarthiring.service.ExternalShiftSearchClient;
import com.smarthiring.service.N8nShiftSearchClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ShiftSearchClientConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.matching.enabled", havingValue = "true", matchIfMissing = true)
    ExternalShiftSearchClient n8nShiftSearchClient(
            @Value("${n8n.shift-search-webhook-url:http://localhost:5678/webhook/staffmatch/shift-search}") String webhookUrl,
            @Value("${n8n.webhook-secret:}") String webhookSecret
    ) {
        return new N8nShiftSearchClient(true, webhookUrl, webhookSecret, "N8N_DEEPSEEK");
    }

    @Bean
    @ConditionalOnProperty(name = "app.matching.enabled", havingValue = "false")
    ExternalShiftSearchClient disabledShiftSearchClient() {
        return (systemPrompt, userPrompt, allowedShiftIds) -> java.util.Optional.empty();
    }
}
