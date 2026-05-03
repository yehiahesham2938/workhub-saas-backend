package com.workhub.saasbackend.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.workhub.saasbackend.config.RabbitMQConfig;
import com.workhub.saasbackend.entity.Job;
import com.workhub.saasbackend.entity.JobStatus;
import com.workhub.saasbackend.repository.JobRepository;

@Component
public class JobConsumer {

	private static final Logger log = LoggerFactory.getLogger(JobConsumer.class);

	private final JobRepository jobRepository;

	public JobConsumer(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
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
		try {
			Job job = jobRepository.findByIdAndTenantId(message.getJobId(), message.getTenantId()).orElse(null);
			if (job == null) {
				log.warn("job not found for tenant; dropping message");
				return;
			}

			log.info("job transition: PENDING -> PROCESSING");
			job.setStatus(JobStatus.PROCESSING);
			jobRepository.save(job);

			Thread.sleep(500);

			log.info("job transition: PROCESSING -> DONE");
			job.setStatus(JobStatus.DONE);
			job.setErrorMessage(null);
			jobRepository.save(job);
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
		}
	}

	private void markFailed(JobMessage message, String errorMessage) {
		jobRepository.findByIdAndTenantId(message.getJobId(), message.getTenantId()).ifPresent(job -> {
			job.setStatus(JobStatus.FAILED);
			job.setErrorMessage(errorMessage);
			jobRepository.save(job);
		});
	}
}
