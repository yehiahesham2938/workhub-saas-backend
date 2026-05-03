package com.workhub.saasbackend.integration;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workhub.saasbackend.entity.UserRole;
import com.workhub.saasbackend.security.JwtService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractTenantIsolationIT {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtService jwtService;

    protected String tokenAAdmin;
    protected String tokenBAdmin;
    protected String tokenAUser;
    protected String tokenAViewer;

    @BeforeEach
    void authenticateTenants() throws Exception {
        tokenAAdmin = loginAndGetToken("admin-a@a.com");
        tokenBAdmin = loginAndGetToken("admin-b@b.com");
        tokenAUser = loginAndGetToken("member-a@a.com");
        tokenAViewer = jwtService.generateToken(UUID.randomUUID().toString(), "tenant-a", UserRole.VIEWER);
    }

    protected String loginAndGetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of("email", email, "password", "password"))))
                .andExpect(status().isOk())
                .andReturn();

        return readJson(result).get("token").asText();
    }

    protected String createProject(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/projects")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn();
        return readJson(result).get("id").asText();
    }

    protected String createTask(String token, String projectId, String status) throws Exception {
        MvcResult result = mockMvc.perform(post("/projects/" + projectId + "/tasks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of("status", status))))
                .andExpect(status().isCreated())
                .andReturn();
        return readJson(result).get("id").asText();
    }

    protected String createWorkspace(String token, String name, String ownerEmail) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/workspaces")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of("name", name, "ownerEmail", ownerEmail))))
                .andExpect(status().isCreated())
                .andReturn();
        return readJson(result).get("id").asText();
    }

    protected String createJob(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/jobs")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isAccepted())
                .andReturn();
        return readJson(result).get("id").asText();
    }

    protected JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected String asJson(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }
}
