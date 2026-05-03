package com.workhub.saasbackend.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class TenantIsolationProjectIT extends AbstractTenantIsolationIT {

    @Test
    void projectEndpoints_enforceCrossTenantIsolation() throws Exception {
        String projectAId = createProject(tokenAAdmin, "tenant-a-project");
        createProject(tokenBAdmin, "tenant-b-project");

        MvcResult listResult = mockMvc.perform(get("/projects")
                        .header("Authorization", bearer(tokenBAdmin)))
                .andExpect(status().isOk())
                .andReturn();

        String listPayload = listResult.getResponse().getContentAsString();
        assertFalse(listPayload.contains(projectAId));

        mockMvc.perform(get("/projects/" + projectAId)
                        .header("Authorization", bearer(tokenBAdmin)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/projects/" + projectAId)
                        .header("Authorization", bearer(tokenBAdmin)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/projects/" + projectAId + "/tasks")
                        .header("Authorization", bearer(tokenBAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(java.util.Map.of("status", "TODO"))))
                .andExpect(status().isNotFound());
    }
}
