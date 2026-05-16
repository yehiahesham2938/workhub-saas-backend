package com.workhub.saasbackend.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workhub.saasbackend.config.RabbitMQConfig;
import com.workhub.saasbackend.dto.response.DeadLetterReplayResponse;
import com.workhub.saasbackend.entity.AuditActionResult;
import com.workhub.saasbackend.entity.AuditEventType;
import com.workhub.saasbackend.entity.Job;
import com.workhub.saasbackend.entity.JobStatus;
import com.workhub.saasbackend.feature.TenantFeature;
import com.workhub.saasbackend.feature.TenantFeatureService;
import com.workhub.saasbackend.messaging.JobMessage;
import com.workhub.saasbackend.messaging.JobProducer;
import com.workhub.saasbackend.observability.BusinessLogger;
import com.workhub.saasbackend.repository.JobRepository;
import com.workhub.saasbackend.security.SecurityActorSupport;
import com.workhub.saasbackend.security.TenantContext;
import com.workhub.saasbackend.service.AuditService;
import com.workhub.saasbackend.service.DeadLetterReplayService;

@Service
public class DeadLetterReplayServiceImpl implements DeadLetterReplayService {

    private static final int MAX_LIMIT = 50;

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final JobRepository jobRepository;
    private final JobProducer jobProducer;
    private final AuditService auditService;
    private final TenantFeatureService tenantFeatureService;

    public DeadLetterReplayServiceImpl(RabbitTemplate rabbitTemplate,
                                       ObjectMapper objectMapper,
                                       JobRepository jobRepository,
                                       JobProducer jobProducer,
                                       AuditService auditService,
                                       TenantFeatureService tenantFeatureService) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.jobRepository = jobRepository;
        this.jobProducer = jobProducer;
        this.auditService = auditService;
        this.tenantFeatureService = tenantFeatureService;
    }

    @Override
    @Transactional
    public DeadLetterReplayResponse replayForCurrentTenant(int limit) {
        tenantFeatureService.requireFeature(TenantFeature.DLQ_REPLAY);
        String tenantId = TenantContext.getRequiredTenantId();
        String actorId = SecurityActorSupport.currentActorId();
        int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);

        int replayed = 0;
        int skipped = 0;
        List<String> details = new ArrayList<>();

        for (int i = 0; i < safeLimit; i++) {
            Message raw = rabbitTemplate.receive(RabbitMQConfig.JOBS_DLQ, 2000);
            if (raw == null) {
                details.add("no more DLQ messages");
                break;
            }

            JobMessage payload = parseMessage(raw);
            if (payload == null || payload.getJobId() == null) {
                skipped++;
                details.add("skipped malformed message");
                continue;
            }

            if (!tenantId.equals(payload.getTenantId())) {
                rabbitTemplate.send(RabbitMQConfig.JOBS_DLQ, raw);
                skipped++;
                details.add("skipped foreign tenant message jobId=" + payload.getJobId());
                continue;
            }

            Optional<Job> jobOpt = jobRepository.findByIdAndTenantId(payload.getJobId(), tenantId);
            if (jobOpt.isEmpty()) {
                skipped++;
                details.add("skipped missing job " + payload.getJobId());
                continue;
            }

            Job job = jobOpt.get();
            if (job.getStatus() == JobStatus.DONE) {
                skipped++;
                details.add("skipped terminal DONE job " + job.getId());
                continue;
            }

            job.setStatus(JobStatus.PENDING);
            job.setErrorMessage(null);
            jobRepository.save(job);
            jobProducer.send(payload);
            replayed++;
            details.add("replayed job " + job.getId());

            auditService.record(tenantId, AuditEventType.ADMIN_ACTION, actorId,
                    "job", job.getId().toString(), AuditActionResult.SUCCESS,
                    "DLQ replay");
            BusinessLogger.info("DLQ_REPLAY", "replayed job from dead-letter queue",
                    "jobId", job.getId(), "tenantId", tenantId);
        }

        auditService.record(tenantId, AuditEventType.ADMIN_ACTION, actorId,
                "dlq", RabbitMQConfig.JOBS_DLQ, AuditActionResult.SUCCESS,
                "replay summary replayed=" + replayed + " skipped=" + skipped);

        return new DeadLetterReplayResponse(safeLimit, replayed, skipped, details);
    }

    private JobMessage parseMessage(Message raw) {
        try {
            return objectMapper.readValue(raw.getBody(), JobMessage.class);
        } catch (Exception ex) {
            return null;
        }
    }
}
