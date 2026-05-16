package com.workhub.saasbackend.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class Phase3AdminOperationsIT extends AbstractTenantIsolationIT {

    @Test
    void adminQuotaUsage_shouldReturnTenantPlanAndUsage() throws Exception {
        createProject(tokenAAdmin, "Quota Inspect " + UUID.randomUUID());

        mockMvc.perform(get("/admin/quotas")
                        .header("Authorization", bearer(tokenAAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("tenant-a"))
                .andExpect(jsonPath("$.plan").value("STARTER"))
                .andExpect(jsonPath("$.projects.used").isNumber());
    }

    @Test
    void adminTenantSummary_shouldReturnCounts() throws Exception {
        createProject(tokenAAdmin, "Summary " + UUID.randomUUID());

        mockMvc.perform(get("/admin/tenant/summary")
                        .header("Authorization", bearer(tokenAAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("tenant-a"))
                .andExpect(jsonPath("$.projectCount").isNumber())
                .andExpect(jsonPath("$.jobCountsByStatus").isMap());
    }

    @Test
    void adminDeadLetterInspection_shouldReturnQueueMetadata() throws Exception {
        mockMvc.perform(get("/admin/queues/dead-letter")
                        .header("Authorization", bearer(tokenAAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueName").value("jobs.dlq"))
                .andExpect(jsonPath("$.jobsQueueName").value("jobs.queue"));
    }

    @Test
    void memberCannotAccessAdminQuotaEndpoint() throws Exception {
        mockMvc.perform(get("/admin/quotas")
                        .header("Authorization", bearer(tokenAUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void tenantBAdminCannotSeeTenantAQuotaUsage() throws Exception {
        createProject(tokenAAdmin, "Tenant A Only");

        mockMvc.perform(get("/admin/quotas")
                        .header("Authorization", bearer(tokenBAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("tenant-b"));
    }
}
