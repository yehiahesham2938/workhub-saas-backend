package com.workhub.saasbackend.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workhub.saasbackend.config.CacheConfig;
import com.workhub.saasbackend.dto.request.CreateWorkspaceRequest;
import com.workhub.saasbackend.dto.response.WorkspaceResponse;
import com.workhub.saasbackend.entity.AuditActionResult;
import com.workhub.saasbackend.entity.AuditEventType;
import com.workhub.saasbackend.entity.Workspace;
import com.workhub.saasbackend.exception.ResourceNotFoundException;
import com.workhub.saasbackend.repository.WorkspaceRepository;
import com.workhub.saasbackend.security.SecurityActorSupport;
import com.workhub.saasbackend.security.TenantContext;
import com.workhub.saasbackend.service.AuditService;
import com.workhub.saasbackend.service.QuotaService;
import com.workhub.saasbackend.service.WorkspaceService;

@Service
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final AuditService auditService;
    private final QuotaService quotaService;

    public WorkspaceServiceImpl(WorkspaceRepository workspaceRepository,
                                AuditService auditService,
                                QuotaService quotaService) {
        this.workspaceRepository = workspaceRepository;
        this.auditService = auditService;
        this.quotaService = quotaService;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.WORKSPACES_CACHE, allEntries = true)
    public WorkspaceResponse createWorkspace(CreateWorkspaceRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();
        quotaService.checkWorkspaceQuota(tenantId);

        Workspace workspace = new Workspace();
        workspace.setTenantId(tenantId);
        workspace.setName(request.getName());
        workspace.setOwnerEmail(request.getOwnerEmail());

        Workspace saved = workspaceRepository.save(workspace);
        auditService.record(tenantId, AuditEventType.WORKSPACE_CREATED, SecurityActorSupport.currentActorId(),
                "workspace", saved.getId().toString(), AuditActionResult.SUCCESS, request.getName());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.WORKSPACES_CACHE,
            key = "T(com.workhub.saasbackend.security.TenantContext).getRequiredTenantId() + ':' + #id")
    public WorkspaceResponse getWorkspace(UUID id) {
        String tenantId = TenantContext.getRequiredTenantId();

        Workspace workspace = workspaceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        auditService.record(tenantId, AuditEventType.WORKSPACE_READ, SecurityActorSupport.currentActorId(),
                "workspace", id.toString(), AuditActionResult.SUCCESS, null);

        return toResponse(workspace);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.WORKSPACES_CACHE,
            key = "T(com.workhub.saasbackend.security.TenantContext).getRequiredTenantId() + ':list'")
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
