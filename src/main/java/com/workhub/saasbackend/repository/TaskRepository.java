package com.workhub.saasbackend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workhub.saasbackend.entity.Task;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    Optional<Task> findByIdAndTenantId(UUID id, String tenantId);
}
