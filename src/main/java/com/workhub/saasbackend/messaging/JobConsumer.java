package com.workhub.saasbackend.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.workhub.saasbackend.config.RabbitMQConfig;
import com.workhub.saasbackend.entity.AuditActionResult;
import com.workhub.saasbackend.entity.AuditEventType;
import com.workhub.saasbackend.entity.Job;
import com.workhub.saasbackend.entity.JobStatus;
import com.workhub.saasbackend.observability.BusinessLogger;
import com.workhub.saasbackend.observability.BusinessMetrics;
import com.workhub.saasbackend.observability.TraceContext;
import com.workhub.saasbackend.observability.TracePropagation;
import com.workhub.saasbackend.repository.JobRepository;
import com.workhub.saasbackend.service.AuditService;

@Component
public class JobConsumer {

	private static final Logger log = LoggerFactory.getLogger(JobConsumer.class);

	private final JobRepository jobRepository;
	private final AuditService auditService;
	private final BusinessMetrics businessMetrics;

	public JobConsumer(JobRepository jobRepository, AuditService auditService, BusinessMetrics businessMetrics) {
		this.jobRepository = jobRepository;
		this.auditService = auditService;
		this.businessMetrics = businessMetrics;
	}

	@RabbitListener(queues = RabbitMQConfig.JOBS_QUEUE)
	@Transactional
	public void handleJobMessage(JobMessage message) {
		if (message == null || message.getJobId() == null || message.getTenantId() == null) {
			log.warn("rejecting malformed job message: {}", message);
			throw new AmqpRejectAndDontRequeueException("malformed JobMessage");
		}

		MDC.put("jobId", String.valueOf(message.getJobId()));
		MDC.put("tenantId", message.getTenantId());
		TracePropagation.restoreFromMessage(message);
		try {
			Job job = jobRepository.findByIdAndTenantId(message.getJobId(), message.getTenantId()).orElse(null);
			if (job == null) {
				log.warn("job not found for tenant; dropping message");
				return;
			}

			if (job.getStatus() == JobStatus.DONE) {
				log.info("job already DONE; skipping duplicate message jobId={}", job.getId());
				return;
			}
			if (job.getStatus() == JobStatus.PROCESSING) {
				log.info("job already PROCESSING; skipping duplicate message jobId={}", job.getId());
				return;
			}
			if (job.getStatus() == JobStatus.FAILED) {
				log.info("job already FAILED; skipping reprocessing jobId={}", job.getId());
				return;
			}

			BusinessLogger.info("JOB_PROCESSING", "job transition PENDING -> PROCESSING",
					"jobId", job.getId(), "tenantId", message.getTenantId());
			job.setStatus(JobStatus.PROCESSING);
			jobRepository.save(job);
			auditService.record(message.getTenantId(), AuditEventType.JOB_PROCESSING, null,
					"job", job.getId().toString(), AuditActionResult.SUCCESS, null);

			Thread.sleep(500);

			job.setStatus(JobStatus.DONE);
			job.setErrorMessage(null);
			jobRepository.save(job);
			businessMetrics.recordJobCompleted(message.getTenantId(), true);
			auditService.record(message.getTenantId(), AuditEventType.JOB_COMPLETED, null,
					"job", job.getId().toString(), AuditActionResult.SUCCESS, null);
			BusinessLogger.info("JOB_COMPLETED", "job transition PROCESSING -> DONE",
					"jobId", job.getId(), "tenantId", message.getTenantId());
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			markFailed(message, "interrupted");
			log.warn("job interrupted", ex);
		} catch (Exception ex) {
			markFailed(message, ex.getMessage());
			log.error("job failed", ex);
		} finally {
			MDC.remove("jobId");
			MDC.remove("tenantId");
			TraceContext.clear();
		}
	}

	private void markFailed(JobMessage message, String errorMessage) {
		jobRepository.findByIdAndTenantId(message.getJobId(), message.getTenantId()).ifPresent(job -> {
			job.setStatus(JobStatus.FAILED);
			job.setErrorMessage(errorMessage);
			jobRepository.save(job);
			businessMetrics.recordJobCompleted(message.getTenantId(), false);
			auditService.record(message.getTenantId(), AuditEventType.JOB_FAILED, null,
					"job", job.getId().toString(), AuditActionResult.FAILURE, errorMessage);
			BusinessLogger.warn("JOB_FAILED", "job processing failed",
					"jobId", job.getId(), "tenantId", message.getTenantId(), "error", errorMessage);
		});
	}
}
