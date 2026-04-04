package com.workhub.saasbackend.messaging;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	public void handleJobMessage(String jobIdString) {
		UUID jobId = UUID.fromString(jobIdString);
		Job job = jobRepository.findById(jobId).orElse(null);
		if (job == null) {
			return;
		}
		try {
			job.setStatus(JobStatus.PROCESSING);
			jobRepository.save(job);
			// Simulate processing time
			Thread.sleep(1500);
			job.setStatus(JobStatus.DONE);
			job.setErrorMessage(null);
			jobRepository.save(job);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			job.setStatus(JobStatus.FAILED);
			job.setErrorMessage("Interrupted");
			jobRepository.save(job);
			log.warn("Job {} interrupted", jobId);
		} catch (Exception ex) {
			job.setStatus(JobStatus.FAILED);
			job.setErrorMessage(ex.getMessage());
			jobRepository.save(job);
			log.error("Job {} failed: {}", jobId, ex.getMessage());
		}
	}
}

