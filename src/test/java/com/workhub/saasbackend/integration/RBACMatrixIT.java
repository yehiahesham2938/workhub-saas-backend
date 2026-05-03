package com.workhub.saasbackend.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class RBACMatrixIT extends AbstractTenantIsolationIT {

    @Test
    void protectedEndpoints_followRbacMatrix_forNoTokenMemberAdmin() throws Exception {
        String projectId = createProject(tokenAAdmin, "rbac-matrix-project");
        String taskId = createTask(tokenAAdmin, projectId, "TODO");
        String workspaceId = createWorkspace(tokenAAdmin, "rbac-matrix-workspace", "owner@a.com");
        String jobId = createJob(tokenAAdmin);
        String deleteProjectId = createProject(tokenAAdmin, "rbac-delete-project");

        assertMatrix(() -> get("/auth/me"), 200, 200);

        assertMatrix(() -> post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonUnchecked(Map.of("name", "rbac-created-project-" + java.util.UUID.randomUUID()))), 201, 201);

        assertMatrix(() -> get("/projects"), 200, 200);
        assertMatrix(() -> get("/projects/" + projectId), 200, 200);

        assertMatrix(() -> post("/projects/" + projectId + "/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonUnchecked(Map.of("status", "IN_PROGRESS"))), 201, 201);

        assertMatrix(() -> patch("/tasks/" + taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonUnchecked(Map.of("status", "DONE"))), 200, 200);

        assertMatrix(() -> post("/api/v1/workspaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonUnchecked(Map.of("name", "rbac-ws-created", "ownerEmail", "ws@a.com"))), 201, 201);

        assertMatrix(() -> get("/api/v1/workspaces"), 200, 200);
        assertMatrix(() -> get("/api/v1/workspaces/" + workspaceId), 200, 200);

        assertMatrix(() -> post("/jobs"), 202, 202);
        assertMatrix(() -> get("/jobs/" + jobId), 200, 200);

        assertMatrix(() -> post("/projects/tx-demo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonUnchecked(Map.of("name", "rbac-tx-demo-" + java.util.UUID.randomUUID(), "taskStatuses", java.util.List.of("TODO")))), 202, 202);

        assertMatrix(() -> delete("/projects/" + deleteProjectId), 403, 204);
    }

    private void assertMatrix(Supplier<MockHttpServletRequestBuilder> requestFactory,
                              int memberExpected,
                              int adminExpected) throws Exception {
        mockMvc.perform(requestFactory.get())
                .andExpect(status().isUnauthorized());

        mockMvc.perform(requestFactory.get()
                        .header("Authorization", bearer(tokenAUser)))
                .andExpect(status().is(memberExpected));

        mockMvc.perform(requestFactory.get()
                        .header("Authorization", bearer(tokenAAdmin)))
                .andExpect(status().is(adminExpected));
    }

    private String asJsonUnchecked(Object body) {
        try {
            return asJson(body);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
