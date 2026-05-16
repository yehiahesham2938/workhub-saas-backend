package com.workhub.saasbackend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workhub.saasbackend.entity.WorkflowExecution;

public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, UUID> {

    Optional<WorkflowExecution> findByIdAndTenantId(UUID id, String tenantId);

    long countByTenantId(String tenantId);
}
