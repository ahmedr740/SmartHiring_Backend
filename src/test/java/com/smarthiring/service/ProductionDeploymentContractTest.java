package com.smarthiring.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionDeploymentContractTest {

    @Test
    void productionComposeRoutesAllAiWebhooksToN8nContainer() throws Exception {
        String compose = Files.readString(Path.of("docker-compose.prod.yml"));

        assertThat(compose)
                .contains("MATCHING_PROVIDER: n8n")
                .contains("N8N_WORKER_MATCH_WEBHOOK_URL: http://n8n:5678/webhook/staffmatch/worker-shift-match")
                .contains("N8N_MANAGER_MATCH_WEBHOOK_URL: http://n8n:5678/webhook/staffmatch/manager-applicant-match")
                .contains("N8N_SHIFT_DRAFT_WEBHOOK_URL: http://n8n:5678/webhook/staffmatch/shift-draft")
                .contains("N8N_SHIFT_SEARCH_WEBHOOK_URL: http://n8n:5678/webhook/staffmatch/shift-search")
                .doesNotContain("N8N_SHIFT_DRAFT_WEBHOOK_URL: http://localhost")
                .doesNotContain("N8N_SHIFT_SEARCH_WEBHOOK_URL: http://localhost");
    }
}
