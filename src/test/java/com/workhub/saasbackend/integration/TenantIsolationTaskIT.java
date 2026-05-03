package com.workhub.saasbackend.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class TenantIsolationTaskIT extends AbstractTenantIsolationIT {

    @Test
    void updateTask_crossTenantReturnsNotFound() throws Exception {
        String projectAId = createProject(tokenAAdmin, "task-host-project-a");
        String taskAId = createTask(tokenAAdmin, projectAId, "TODO");

        mockMvc.perform(patch("/tasks/" + taskAId)
                        .header("Authorization", bearer(tokenBAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of("status", "DONE"))))
                .andExpect(status().isNotFound());
    }
}
