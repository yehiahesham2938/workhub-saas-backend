package com.workhub.saasbackend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;

import com.workhub.saasbackend.config.CacheConfig;

import com.workhub.saasbackend.entity.AuditEventType;
import com.workhub.saasbackend.entity.JobStatus;
import com.workhub.saasbackend.entity.WorkflowStatus;
import com.workhub.saasbackend.repository.JobRepository;
import com.workhub.saasbackend.repository.AuditEventRepository;
import com.workhub.saasbackend.repository.ProjectRepository;

class Phase3EnterpriseIT extends AbstractTenantIsolationIT {

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void projectCreate_shouldWriteAuditRecord() throws Exception {
        createProject(tokenAAdmin, "Audit Project " + UUID.randomUUID());

        boolean found = auditEventRepository.findByTenantIdAndEventTypeOrderByOccurredAtDesc(
                        "tenant-a", AuditEventType.PROJECT_CREATED)
                .stream()
                .anyMatch(event -> event.getDetails() != null && event.getDetails().contains("Audit Project"));
        assertThat(found).isTrue();
    }

    @Test
    void crossTenantAccess_shouldWriteDeniedAuditRecord() throws Exception {
        String projectId = createProject(tokenAAdmin, "Tenant A Secret");

        mockMvc.perform(get("/projects/" + projectId)
                        .header("Authorization", bearer(tokenBAdmin)))
                .andExpect(status().isNotFound());

        boolean denied = auditEventRepository.findByTenantIdAndEventTypeOrderByOccurredAtDesc(
                        "tenant-b", AuditEventType.CROSS_TENANT_DENIED)
                .stream()
                .anyMatch(event -> "project".equals(event.getResourceType()));
        assertThat(denied).isTrue();
    }

    @Test
    void jobCreate_shouldWriteJobCreatedAuditRecord() throws Exception {
        String jobId = createJob(tokenAAdmin, "audit-job-" + UUID.randomUUID());

        boolean found = auditEventRepository.findByTenantIdAndEventTypeOrderByOccurredAtDesc(
                        "tenant-a", AuditEventType.JOB_CREATED)
                .stream()
                .anyMatch(event -> jobId.equals(event.getResourceId()));
        assertThat(found).isTrue();
        assertThat(jobRepository.findByIdAndTenantId(UUID.fromString(jobId), "tenant-a"))
                .map(job -> job.getStatus())
                .contains(JobStatus.PENDING);
    }

    @Test
    void duplicateJobSubmission_withSameIdempotencyKey_returnsSameJob() throws Exception {
        String key = "idem-" + UUID.randomUUID();
        String firstId = createJob(tokenAAdmin, key);
        String secondId = createJob(tokenAAdmin, key);
        assertThat(secondId).isEqualTo(firstId);
    }

    @Test
    void sagaFailure_shouldCompensateAndLeaveNoProject() throws Exception {
        long before = projectRepository.countByTenantId("tenant-a");

        mockMvc.perform(post("/projects/provision-saga")
                        .header("Authorization", bearer(tokenAAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "name", "Saga Fail " + UUID.randomUUID(),
                                "defaultTaskStatuses", List.of("TODO", "IN_PROGRESS"),
                                "simulateFailure", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(WorkflowStatus.COMPENSATED.name()));

        assertThat(projectRepository.countByTenantId("tenant-a")).isEqualTo(before);
    }

    @Test
    void workspaceQuotaExceeded_shouldReturn429() throws Exception {
        createWorkspace(tokenAAdmin, "ws-1", "owner@a.com");

        mockMvc.perform(post("/api/v1/workspaces")
                        .header("Authorization", bearer(tokenAAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of("name", "ws-2", "ownerEmail", "owner2@a.com"))))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void sagaSuccess_shouldCompleteWithProjectAndTasks() throws Exception {
        long before = projectRepository.countByTenantId("tenant-a");

        mockMvc.perform(post("/projects/provision-saga")
                        .header("Authorization", bearer(tokenAAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "name", "Saga OK " + UUID.randomUUID(),
                                "defaultTaskStatuses", List.of("TODO"),
                                "simulateFailure", false
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(WorkflowStatus.COMPLETED.name()));

        assertThat(projectRepository.countByTenantId("tenant-a")).isEqualTo(before + 1);
    }

    @Test
    void projectGet_shouldPopulateTenantScopedCache() throws Exception {
        String projectId = createProject(tokenAAdmin, "Cache " + UUID.randomUUID());

        mockMvc.perform(get("/projects/" + projectId)
                        .header("Authorization", bearer(tokenAAdmin)))
                .andExpect(status().isOk());

        var cache = cacheManager.getCache(CacheConfig.PROJECTS_CACHE);
        assertThat(cache).isNotNull();
        assertThat(cache.get("tenant-a:" + projectId)).isNotNull();
    }

    @Test
    void adminCanListAuditEvents() throws Exception {
        createProject(tokenAAdmin, "Admin Audit " + UUID.randomUUID());

        mockMvc.perform(get("/admin/audit")
                        .header("Authorization", bearer(tokenAAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
