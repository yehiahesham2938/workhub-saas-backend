package com.workhub.saasbackend.service.impl;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workhub.saasbackend.config.QuotaProperties;
import com.workhub.saasbackend.config.QuotaProperties.PlanLimits;
import com.workhub.saasbackend.config.RabbitMQConfig;
import com.workhub.saasbackend.config.TenantPlanResolver;
import com.workhub.saasbackend.dto.response.DeadLetterQueueResponse;
import com.workhub.saasbackend.dto.response.QuotaUsageResponse;
import com.workhub.saasbackend.dto.response.TenantSummaryResponse;
import com.workhub.saasbackend.entity.JobStatus;
import com.workhub.saasbackend.entity.TenantPlan;
import com.workhub.saasbackend.repository.AuditEventRepository;
import com.workhub.saasbackend.repository.JobRepository;
import com.workhub.saasbackend.repository.ProjectRepository;
import com.workhub.saasbackend.repository.WorkflowExecutionRepository;
import com.workhub.saasbackend.repository.WorkspaceRepository;
import com.workhub.saasbackend.security.TenantContext;
import com.workhub.saasbackend.service.OperationalService;

@Service
public class OperationalServiceImpl implements OperationalService {

    private static final EnumSet<JobStatus> OPEN_JOB_STATUSES = EnumSet.of(JobStatus.PENDING, JobStatus.PROCESSING);

    private final QuotaProperties quotaProperties;
    private final TenantPlanResolver tenantPlanResolver;
    private final WorkspaceRepository workspaceRepository;
    private final ProjectRepository projectRepository;
    private final JobRepository jobRepository;
    private final AuditEventRepository auditEventRepository;
    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final RabbitAdmin rabbitAdmin;

    public OperationalServiceImpl(QuotaProperties quotaProperties,
                                  TenantPlanResolver tenantPlanResolver,
                                  WorkspaceRepository workspaceRepository,
                                  ProjectRepository projectRepository,
                                  JobRepository jobRepository,
                                  AuditEventRepository auditEventRepository,
                                  WorkflowExecutionRepository workflowExecutionRepository,
                                  RabbitAdmin rabbitAdmin) {
        this.quotaProperties = quotaProperties;
        this.tenantPlanResolver = tenantPlanResolver;
        this.workspaceRepository = workspaceRepository;
        this.projectRepository = projectRepository;
        this.jobRepository = jobRepository;
        this.auditEventRepository = auditEventRepository;
        this.workflowExecutionRepository = workflowExecutionRepository;
        this.rabbitAdmin = rabbitAdmin;
    }

    @Override
    @Transactional(readOnly = true)
    public QuotaUsageResponse getQuotaUsage() {
        String tenantId = TenantContext.getRequiredTenantId();
        TenantPlan plan = tenantPlanResolver.resolvePlan(tenantId);
        PlanLimits limits = quotaProperties.limitsFor(plan);

        long workspaces = workspaceRepository.countByTenantId(tenantId);
        long projects = projectRepository.countByTenantId(tenantId);
        long openJobs = jobRepository.countByTenantIdAndStatusIn(tenantId, OPEN_JOB_STATUSES);

        return new QuotaUsageResponse(
                tenantId,
                plan,
                usage(workspaces, limits.maxWorkspaces()),
                usage(projects, limits.maxProjects()),
                usage(openJobs, limits.maxOpenJobs()),
                new QuotaUsageResponse.UsageLimit(0, limits.maxTasksPerProject(), false)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TenantSummaryResponse getTenantSummary() {
        String tenantId = TenantContext.getRequiredTenantId();
        TenantPlan plan = tenantPlanResolver.resolvePlan(tenantId);

        Map<String, Long> jobCounts = new LinkedHashMap<>();
        for (JobStatus status : JobStatus.values()) {
            jobCounts.put(status.name(), jobRepository.countByTenantIdAndStatus(tenantId, status));
        }

        return new TenantSummaryResponse(
                tenantId,
                plan,
                workspaceRepository.countByTenantId(tenantId),
                projectRepository.countByTenantId(tenantId),
                jobCounts,
                auditEventRepository.countByTenantId(tenantId),
                workflowExecutionRepository.countByTenantId(tenantId)
        );
    }

    @Override
    public DeadLetterQueueResponse inspectDeadLetterQueue() {
        try {
            Properties dlqProps = rabbitAdmin.getQueueProperties(RabbitMQConfig.JOBS_DLQ);
            Properties jobsProps = rabbitAdmin.getQueueProperties(RabbitMQConfig.JOBS_QUEUE);
            return new DeadLetterQueueResponse(
                    RabbitMQConfig.JOBS_DLQ,
                    queueDepth(dlqProps),
                    RabbitMQConfig.JOBS_QUEUE,
                    queueDepth(jobsProps),
                    "Failed job messages land in the DLQ after listener retries are exhausted."
            );
        } catch (Exception ex) {
            return new DeadLetterQueueResponse(
                    RabbitMQConfig.JOBS_DLQ,
                    0,
                    RabbitMQConfig.JOBS_QUEUE,
                    0,
                    "RabbitMQ inspection unavailable: " + ex.getMessage()
            );
        }
    }

    private QuotaUsageResponse.UsageLimit usage(long used, int max) {
        return new QuotaUsageResponse.UsageLimit((int) used, max, used >= max);
    }

    private int queueDepth(Properties properties) {
        if (properties == null) {
            return 0;
        }
        Object count = properties.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
        return count instanceof Integer integer ? integer : 0;
    }
}
