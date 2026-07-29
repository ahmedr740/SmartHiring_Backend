package com.smarthiring.config;

import com.smarthiring.service.ExternalShiftDraftClient;
import com.smarthiring.service.N8nShiftDraftClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ShiftDraftClientConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.matching.enabled", havingValue = "true", matchIfMissing = true)
    ExternalShiftDraftClient n8nShiftDraftClient(
            @Value("${n8n.shift-draft-webhook-url:http://localhost:5678/webhook/staffmatch/shift-draft}") String webhookUrl,
            @Value("${n8n.webhook-secret:}") String webhookSecret
    ) {
        return new N8nShiftDraftClient(true, webhookUrl, webhookSecret, "N8N_DEEPSEEK");
    }

    @Bean
    @ConditionalOnProperty(name = "app.matching.enabled", havingValue = "false")
    ExternalShiftDraftClient disabledShiftDraftClient() {
        return (systemPrompt, userPrompt) -> java.util.Optional.empty();
    }
}
