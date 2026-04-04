package com.workhub.saasbackend.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workhub.saasbackend.dto.response.JobResponse;
import com.workhub.saasbackend.entity.Job;
import com.workhub.saasbackend.entity.JobStatus;
import com.workhub.saasbackend.exception.ResourceNotFoundException;
import com.workhub.saasbackend.messaging.JobProducer;
import com.workhub.saasbackend.repository.JobRepository;
import com.workhub.saasbackend.security.TenantContext;
import com.workhub.saasbackend.service.JobService;
import org.springframework.security.access.AccessDeniedException;

@Service
public class JobServiceImpl implements JobService {

	private final JobRepository jobRepository;
	private final JobProducer jobProducer;

	public JobServiceImpl(JobRepository jobRepository, JobProducer jobProducer) {
		this.jobRepository = jobRepository;
		this.jobProducer = jobProducer;
	}

	@Override
	@Transactional
	public JobResponse createJob() {
		String tenantId = TenantContext.getRequiredTenantId();
		Job job = new Job();
		job.setTenantId(tenantId);
		job.setStatus(JobStatus.PENDING);
		Job saved = jobRepository.save(job);
		jobProducer.send(saved.getId());
		return toResponse(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public JobResponse getJob(UUID id) {
		String tenantId = TenantContext.getRequiredTenantId();
		Job job = jobRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Job not found"));
		if (!tenantId.equals(job.getTenantId())) {
			throw new AccessDeniedException("Access denied: tenant mismatch");
		}
		return toResponse(job);
	}

	private JobResponse toResponse(Job job) {
		return new JobResponse(
				job.getId(),
				job.getStatus(),
				job.getTenantId(),
				job.getErrorMessage(),
				job.getCreatedAt(),
				job.getUpdatedAt()
		);
	}
}

