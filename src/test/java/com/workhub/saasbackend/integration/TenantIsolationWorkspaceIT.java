package com.workhub.saasbackend.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class TenantIsolationWorkspaceIT extends AbstractTenantIsolationIT {

    @Test
    void workspaceEndpoints_enforceCrossTenantIsolation() throws Exception {
        String workspaceAId = createWorkspace(tokenAAdmin, "workspace-a", "admin-a@a.com");
        createWorkspace(tokenBAdmin, "workspace-b", "admin-b@b.com");

        MvcResult listResult = mockMvc.perform(get("/api/v1/workspaces")
                        .header("Authorization", bearer(tokenBAdmin)))
                .andExpect(status().isOk())
                .andReturn();

        String listPayload = listResult.getResponse().getContentAsString();
        assertFalse(listPayload.contains(workspaceAId));

        mockMvc.perform(get("/api/v1/workspaces/" + workspaceAId)
                        .header("Authorization", bearer(tokenBAdmin)))
                .andExpect(status().isNotFound());
    }
}
