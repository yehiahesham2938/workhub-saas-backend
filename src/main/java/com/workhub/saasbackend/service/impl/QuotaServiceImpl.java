package com.workhub.saasbackend.service.impl;

import java.util.EnumSet;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.workhub.saasbackend.config.QuotaProperties;
import com.workhub.saasbackend.config.QuotaProperties.PlanLimits;
import com.workhub.saasbackend.config.TenantPlanResolver;
import com.workhub.saasbackend.entity.AuditActionResult;
import com.workhub.saasbackend.entity.AuditEventType;
import com.workhub.saasbackend.entity.JobStatus;
import com.workhub.saasbackend.entity.TenantPlan;
import com.workhub.saasbackend.exception.QuotaExceededException;
import com.workhub.saasbackend.repository.JobRepository;
import com.workhub.saasbackend.repository.ProjectRepository;
import com.workhub.saasbackend.repository.TaskRepository;
import com.workhub.saasbackend.repository.WorkspaceRepository;
import com.workhub.saasbackend.observability.BusinessMetrics;
import com.workhub.saasbackend.service.AuditService;
import com.workhub.saasbackend.service.QuotaService;

@Service
public class QuotaServiceImpl implements QuotaService {

    private static final EnumSet<JobStatus> OPEN_JOB_STATUSES = EnumSet.of(JobStatus.PENDING, JobStatus.PROCESSING);

    private final QuotaProperties quotaProperties;
    private final TenantPlanResolver tenantPlanResolver;
    private final WorkspaceRepository workspaceRepository;
    private final ProjectRepository projectRepository;
    private final JobRepository jobRepository;
    private final TaskRepository taskRepository;
    private final AuditService auditService;
    private final BusinessMetrics businessMetrics;

    public QuotaServiceImpl(QuotaProperties quotaProperties,
                              TenantPlanResolver tenantPlanResolver,
                              WorkspaceRepository workspaceRepository,
                              ProjectRepository projectRepository,
                              JobRepository jobRepository,
                              TaskRepository taskRepository,
                              AuditService auditService,
                              BusinessMetrics businessMetrics) {
        this.quotaProperties = quotaProperties;
        this.tenantPlanResolver = tenantPlanResolver;
        this.workspaceRepository = workspaceRepository;
        this.projectRepository = projectRepository;
        this.jobRepository = jobRepository;
        this.taskRepository = taskRepository;
        this.auditService = auditService;
        this.businessMetrics = businessMetrics;
    }

    @Override
    public void checkWorkspaceQuota(String tenantId) {
        PlanLimits limits = limitsFor(tenantId);
        long count = workspaceRepository.countByTenantId(tenantId);
        if (count >= limits.maxWorkspaces()) {
            auditQuotaExceeded(tenantId, "workspace", count, limits.maxWorkspaces());
            throw new QuotaExceededException("Workspace quota exceeded for plan");
        }
    }

    @Override
    public void checkProjectQuota(String tenantId) {
        PlanLimits limits = limitsFor(tenantId);
        long count = projectRepository.countByTenantId(tenantId);
        if (count >= limits.maxProjects()) {
            auditQuotaExceeded(tenantId, "project", count, limits.maxProjects());
            throw new QuotaExceededException("Project quota exceeded for plan");
        }
    }

    @Override
    public void checkOpenJobQuota(String tenantId) {
        PlanLimits limits = limitsFor(tenantId);
        long count = jobRepository.countByTenantIdAndStatusIn(tenantId, OPEN_JOB_STATUSES);
        if (count >= limits.maxOpenJobs()) {
            auditQuotaExceeded(tenantId, "openJob", count, limits.maxOpenJobs());
            throw new QuotaExceededException("Open job quota exceeded for plan");
        }
    }

    @Override
    public void checkTaskQuota(String tenantId, UUID projectId) {
        PlanLimits limits = limitsFor(tenantId);
        long count = taskRepository.countByTenantIdAndProjectId(tenantId, projectId);
        if (count >= limits.maxTasksPerProject()) {
            auditQuotaExceeded(tenantId, "task", count, limits.maxTasksPerProject());
            throw new QuotaExceededException("Task quota exceeded for project");
        }
    }

    private PlanLimits limitsFor(String tenantId) {
        TenantPlan plan = tenantPlanResolver.resolvePlan(tenantId);
        return quotaProperties.limitsFor(plan);
    }

    private void auditQuotaExceeded(String tenantId, String resource, long current, int max) {
        businessMetrics.recordQuotaExceeded(tenantId, resource);
        auditService.record(
                tenantId,
                AuditEventType.QUOTA_EXCEEDED,
                null,
                resource,
                null,
                AuditActionResult.DENIED,
                "current=" + current + ", max=" + max
        );
    }
}
