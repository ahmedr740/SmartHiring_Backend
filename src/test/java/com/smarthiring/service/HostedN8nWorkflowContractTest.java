package com.smarthiring.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HostedN8nWorkflowContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void hostedAiWorkflowsUseDeepSeekAndContainNoCredentialMaterial() throws Exception {
        for (String filename : List.of(
                "hosted-worker-shift-match-deepseek.json",
                "hosted-manager-applicant-match-deepseek.json",
                "hosted-shift-draft-deepseek.json",
                "hosted-shift-search-deepseek.json"
        )) {
            String workflow = readWorkflow(filename);
            JsonNode root = objectMapper.readTree(workflow);

            assertThat(workflow)
                    .contains("deepseek-v4-flash")
                    .contains("N8N_DEEPSEEK")
                    .contains("json_object")
                    .doesNotContain("$env")
                    .doesNotContain("\"credentials\"")
                    .doesNotContain("DEEPSEEK_API_KEY")
                    .doesNotContain("N8N_WEBHOOK_SECRET");
            assertThat(hasHeaderAuthenticatedWebhook(root)).isTrue();
        }
    }

    @Test
    void hostedNotificationWorkflowUsesCredentialProtectedWebhook() throws Exception {
        String workflow = readWorkflow("hosted-notification-email.json");
        JsonNode root = objectMapper.readTree(workflow);

        assertThat(workflow)
                .doesNotContain("$env")
                .doesNotContain("\"credentials\"")
                .doesNotContain("N8N_WEBHOOK_SECRET");
        assertThat(hasHeaderAuthenticatedWebhook(root)).isTrue();
    }

    private boolean hasHeaderAuthenticatedWebhook(JsonNode root) {
        for (JsonNode node : root.path("nodes")) {
            if ("n8n-nodes-base.webhook".equals(node.path("type").asText())
                    && "headerAuth".equals(node.path("parameters").path("authentication").asText())) {
                return true;
            }
        }
        return false;
    }

    private String readWorkflow(String filename) throws Exception {
        return Files.readString(Path.of("docs", "n8n", "workflows", filename));
    }
}
