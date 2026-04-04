package com.workhub.saasbackend.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import com.workhub.saasbackend.dto.request.CreateWorkspaceRequest;
import com.workhub.saasbackend.dto.response.WorkspaceResponse;
import com.workhub.saasbackend.entity.Workspace;
import com.workhub.saasbackend.exception.ResourceNotFoundException;
import com.workhub.saasbackend.repository.WorkspaceRepository;
import com.workhub.saasbackend.security.TenantContext;
import com.workhub.saasbackend.service.WorkspaceService;

@Service
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;

    public WorkspaceServiceImpl(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Override
    @Transactional
    public WorkspaceResponse createWorkspace(CreateWorkspaceRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();

        Workspace workspace = new Workspace();
        workspace.setTenantId(tenantId);
        workspace.setName(request.getName());
        workspace.setOwnerEmail(request.getOwnerEmail());

        Workspace saved = workspaceRepository.save(workspace);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceResponse getWorkspace(UUID id) {
        String tenantId = TenantContext.getRequiredTenantId();

        Workspace workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
        if (!tenantId.equals(workspace.getTenantId())) {
            throw new AccessDeniedException("Access denied: tenant mismatch");
        }

        return toResponse(workspace);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listWorkspaces() {
        String tenantId = TenantContext.getRequiredTenantId();

        return workspaceRepository.findAllByTenantId(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private WorkspaceResponse toResponse(Workspace workspace) {
        return new WorkspaceResponse(
            workspace.getId(),
            workspace.getName(),
            workspace.getOwnerEmail(),
            workspace.getTenantId(),
            workspace.getCreatedAt(),
            workspace.getUpdatedAt()
        );
    }
}
