package com.workhub.saasbackend.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workhub.saasbackend.entity.Job;
import com.workhub.saasbackend.entity.JobStatus;

public interface JobRepository extends JpaRepository<Job, UUID> {

	Optional<Job> findByIdAndTenantId(UUID id, String tenantId);

	Optional<Job> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey);

	long countByTenantIdAndStatusIn(String tenantId, Collection<JobStatus> statuses);

	long countByTenantId(String tenantId);

	long countByTenantIdAndStatus(String tenantId, JobStatus status);
}

