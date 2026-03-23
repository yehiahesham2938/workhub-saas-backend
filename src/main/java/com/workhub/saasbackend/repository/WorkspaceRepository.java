package com.workhub.saasbackend.repository;

import com.workhub.saasbackend.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    List<Workspace> findAllByTenantId(String tenantId);

    Optional<Workspace> findByIdAndTenantId(UUID id, String tenantId);
}
