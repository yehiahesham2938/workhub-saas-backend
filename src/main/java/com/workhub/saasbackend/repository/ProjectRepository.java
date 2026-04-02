package com.workhub.saasbackend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.workhub.saasbackend.entity.Project;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Page<Project> findAllByTenantId(String tenantId, Pageable pageable);

    Optional<Project> findByIdAndTenantId(UUID id, String tenantId);
}
