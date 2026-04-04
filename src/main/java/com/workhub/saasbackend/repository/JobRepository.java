package com.workhub.saasbackend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workhub.saasbackend.entity.Job;

public interface JobRepository extends JpaRepository<Job, UUID> {

	Optional<Job> findByIdAndTenantId(UUID id, String tenantId);
}

