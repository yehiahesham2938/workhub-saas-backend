package com.workhub.saasbackend.service.impl;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workhub.saasbackend.dto.request.CreateProjectRequest;
import com.workhub.saasbackend.dto.response.PagedResponse;
import com.workhub.saasbackend.dto.response.ProjectResponse;
import com.workhub.saasbackend.entity.Project;
import com.workhub.saasbackend.exception.ResourceNotFoundException;
import com.workhub.saasbackend.repository.ProjectRepository;
import com.workhub.saasbackend.security.TenantContext;
import com.workhub.saasbackend.service.ProjectService;

@Service
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectServiceImpl(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();

        Project project = new Project();
        project.setTenantId(tenantId);
        project.setName(request.getName());
        project.setCreatedBy(getCurrentUserId());

        Project saved = projectRepository.save(project);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProjectResponse> listProjects(Pageable pageable) {
        String tenantId = TenantContext.getRequiredTenantId();

        Page<Project> page = projectRepository.findAllByTenantId(tenantId, pageable);
        return new PagedResponse<>(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID id) {
        String tenantId = TenantContext.getRequiredTenantId();

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        if (!tenantId.equals(project.getTenantId())) {
            throw new AccessDeniedException("Access denied: tenant mismatch");
        }

        return toResponse(project);
    }

    @Override
    @Transactional
    public void deleteProject(UUID id) {
        String tenantId = TenantContext.getRequiredTenantId();
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        if (!tenantId.equals(project.getTenantId())) {
            throw new AccessDeniedException("Access denied: tenant mismatch");
        }
        projectRepository.delete(project);
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalArgumentException("Authenticated user ID is missing");
        }

        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Authenticated user ID must be a valid UUID");
        }
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getCreatedBy(),
                project.getTenantId(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
