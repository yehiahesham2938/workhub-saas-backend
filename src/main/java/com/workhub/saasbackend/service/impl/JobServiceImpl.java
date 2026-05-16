package com.workhub.saasbackend.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workhub.saasbackend.dto.request.CreateJobRequest;
import com.workhub.saasbackend.dto.response.JobResponse;
import com.workhub.saasbackend.entity.AuditActionResult;
import com.workhub.saasbackend.entity.AuditEventType;
import com.workhub.saasbackend.entity.Job;
import com.workhub.saasbackend.entity.JobStatus;
import com.workhub.saasbackend.exception.ResourceNotFoundException;
import com.workhub.saasbackend.messaging.JobMessage;
import com.workhub.saasbackend.messaging.JobProducer;
import com.workhub.saasbackend.observability.BusinessLogger;
import com.workhub.saasbackend.observability.BusinessMetrics;
import com.workhub.saasbackend.repository.JobRepository;
import com.workhub.saasbackend.security.SecurityActorSupport;
import com.workhub.saasbackend.security.TenantContext;
import com.workhub.saasbackend.service.AuditService;
import com.workhub.saasbackend.service.JobService;
import com.workhub.saasbackend.service.QuotaService;

@Service
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final JobProducer jobProducer;
    private final AuditService auditService;
    private final QuotaService quotaService;
    private final BusinessMetrics businessMetrics;

    public JobServiceImpl(JobRepository jobRepository,
                          JobProducer jobProducer,
                          AuditService auditService,
                          QuotaService quotaService,
                          BusinessMetrics businessMetrics) {
        this.jobRepository = jobRepository;
        this.jobProducer = jobProducer;
        this.auditService = auditService;
        this.quotaService = quotaService;
        this.businessMetrics = businessMetrics;
    }

    @Override
    @Transactional
    public JobResponse createJob(CreateJobRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();
        String actorId = SecurityActorSupport.currentActorId();
        String idempotencyKey = request != null ? request.getIdempotencyKey() : null;

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return jobRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey)
                    .map(existing -> {
                        auditService.record(tenantId, AuditEventType.JOB_CREATED, actorId,
                                "job", existing.getId().toString(), AuditActionResult.SUCCESS,
                                "idempotent replay");
                        BusinessLogger.info("JOB_IDEMPOTENT_REPLAY", "returning existing job",
                                "jobId", existing.getId(), "tenantId", tenantId);
                        return toResponse(existing);
                    })
                    .orElseGet(() -> createAndPublish(tenantId, actorId, idempotencyKey));
        }

        return createAndPublish(tenantId, actorId, null);
    }

    private JobResponse createAndPublish(String tenantId, String actorId, String idempotencyKey) {
        quotaService.checkOpenJobQuota(tenantId);

        Job job = new Job();
        job.setTenantId(tenantId);
        job.setStatus(JobStatus.PENDING);
        job.setErrorMessage(null);
        job.setIdempotencyKey(idempotencyKey);

        Job saved = jobRepository.save(job);
        businessMetrics.recordJobCreated(tenantId);
        auditService.record(tenantId, AuditEventType.JOB_CREATED, actorId,
                "job", saved.getId().toString(), AuditActionResult.SUCCESS, null);
        BusinessLogger.info("JOB_CREATED", "job persisted and queued",
                "jobId", saved.getId(), "tenantId", tenantId, "status", JobStatus.PENDING);

        boolean published = jobProducer.send(new JobMessage(saved.getId(), tenantId));
        if (!published) {
            businessMetrics.recordJobPublishFailed(tenantId);
            auditService.record(tenantId, AuditEventType.JOB_PUBLISH_FAILED, actorId,
                    "job", saved.getId().toString(), AuditActionResult.FAILURE,
                    "RabbitMQ publish failed; job remains PENDING");
            BusinessLogger.warn("JOB_PUBLISH_FAILED", "rabbit publish failed",
                    "jobId", saved.getId(), "tenantId", tenantId);
        }

        return toResponse(saved);
    }

    @Override
    @Transactional
    public JobResponse retryJob(UUID id) {
        String tenantId = TenantContext.getRequiredTenantId();
        String actorId = SecurityActorSupport.currentActorId();

        Job job = jobRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (job.getStatus() == JobStatus.DONE || job.getStatus() == JobStatus.PROCESSING) {
            throw new IllegalArgumentException("Job cannot be retried in status " + job.getStatus());
        }

        job.setStatus(JobStatus.PENDING);
        job.setErrorMessage(null);
        Job saved = jobRepository.save(job);
        businessMetrics.recordJobRetry(tenantId);

        boolean published = jobProducer.send(new JobMessage(saved.getId(), tenantId));
        auditService.record(tenantId, AuditEventType.ADMIN_ACTION, actorId,
                "job", saved.getId().toString(), AuditActionResult.SUCCESS,
                "manual retry; published=" + published);
        BusinessLogger.info("JOB_RETRY", "admin retried job",
                "jobId", saved.getId(), "tenantId", tenantId, "published", published);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponse getJob(UUID id) {
        String tenantId = TenantContext.getRequiredTenantId();
        Job job = jobRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        return toResponse(job);
    }

    private JobResponse toResponse(Job job) {
        return new JobResponse(
                job.getId(),
                job.getStatus(),
                job.getTenantId(),
                job.getErrorMessage(),
                job.getIdempotencyKey(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
