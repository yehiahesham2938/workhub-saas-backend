package com.workhub.saasbackend.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class SecurityResponseBehaviorIT extends AbstractTenantIsolationIT {

    @Test
    void protectedEndpoint_withoutToken_returns401ApiError() throws Exception {
        mockMvc.perform(get("/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/projects"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void adminOnlyEndpoint_withValidNonAdminToken_returns403ApiError() throws Exception {
        String projectId = createProject(tokenAAdmin, "authz-check-project");

        mockMvc.perform(delete("/projects/" + projectId)
                        .header("Authorization", bearer(tokenAUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value("/projects/" + projectId))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void strictTenantIsolation_withValidWrongTenantToken_returns404ApiError() throws Exception {
        String projectAId = createProject(tokenAAdmin, "tenant-a-proj");

        mockMvc.perform(get("/projects/" + projectAId)
                        .header("Authorization", bearer(tokenBAdmin)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/projects/" + projectAId))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
