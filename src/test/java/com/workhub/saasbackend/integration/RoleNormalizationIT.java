package com.workhub.saasbackend.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class RoleNormalizationIT extends AbstractTenantIsolationIT {

    @Test
    void adminAndUserCanAccessUserProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", bearer(tokenAAdmin)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", bearer(tokenAUser)))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanDeleteButUserCannotDeleteProject() throws Exception {
        String projectId = createProject(tokenAAdmin, "rbac-delete-check");

        mockMvc.perform(delete("/projects/" + projectId)
                        .header("Authorization", bearer(tokenAUser)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/projects/" + projectId)
                        .header("Authorization", bearer(tokenAAdmin)))
                .andExpect(status().isNoContent());
    }

    @Test
    void viewerCannotAccessAdminUserProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/projects")
                        .header("Authorization", bearer(tokenAViewer)))
                .andExpect(status().isForbidden());
    }
}
