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
import com.workhub.saasbackend.repository.TaskRepository;
import com.workhub.saasbackend.security.TenantContext;
import com.workhub.saasbackend.service.ProjectService;
import com.workhub.saasbackend.dto.request.CreateProjectWithTasksRequest;
import com.workhub.saasbackend.entity.Task;
import com.workhub.saasbackend.entity.TaskStatus;
import com.workhub.saasbackend.dto.shared.TaskStatusDto;

@Service
public class JobServiceImpl  implements JobService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public JobServiceImpl(ProjectRepository projectRepository, TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
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

    @Override
    @Transactional
    public void createProjectWithTasksAndRollback(CreateProjectWithTasksRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();

        Project project = new Project();
        project.setTenantId(tenantId);
        project.setName(request.getName());
        project.setCreatedBy(getCurrentUserId());
        Project savedProject = projectRepository.save(project);

        int index = 0;
        for (TaskStatusDto statusDto : request.getTaskStatuses()) {
            if (index == 1) {
                // Force an unchecked exception after creating the first task
                throw new RuntimeException("Simulated failure to demonstrate transaction rollback");
            }
            Task task = new Task();
            task.setTenantId(tenantId);
            task.setProject(savedProject);
            task.setStatus(TaskStatus.valueOf(statusDto.name()));
            taskRepository.save(task);
            index++;
        }
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
