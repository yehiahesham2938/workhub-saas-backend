package com.workhub.saasbackend.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workhub.saasbackend.dto.request.CreateTaskRequest;
import com.workhub.saasbackend.dto.request.UpdateTaskRequest;
import com.workhub.saasbackend.dto.response.TaskResponse;
import com.workhub.saasbackend.dto.shared.TaskStatusDto;
import com.workhub.saasbackend.entity.AuditActionResult;
import com.workhub.saasbackend.entity.AuditEventType;
import com.workhub.saasbackend.entity.Project;
import com.workhub.saasbackend.entity.Task;
import com.workhub.saasbackend.entity.TaskStatus;
import com.workhub.saasbackend.exception.ResourceNotFoundException;
import com.workhub.saasbackend.repository.ProjectRepository;
import com.workhub.saasbackend.repository.TaskRepository;
import com.workhub.saasbackend.security.SecurityActorSupport;
import com.workhub.saasbackend.security.TenantContext;
import com.workhub.saasbackend.service.AuditService;
import com.workhub.saasbackend.service.QuotaService;
import com.workhub.saasbackend.service.TaskService;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final AuditService auditService;
    private final QuotaService quotaService;

    public TaskServiceImpl(TaskRepository taskRepository,
                           ProjectRepository projectRepository,
                           AuditService auditService,
                           QuotaService quotaService) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.auditService = auditService;
        this.quotaService = quotaService;
    }

    @Override
    @Transactional
    public TaskResponse createTask(UUID projectId, CreateTaskRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();

        Project project = projectRepository.findByIdAndTenantId(projectId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        quotaService.checkTaskQuota(tenantId, projectId);

        Task task = new Task();
        task.setTenantId(tenantId);
        task.setProject(project);
        task.setStatus(toEntityStatus(request.getStatus()));

        Task saved = taskRepository.save(task);
        auditService.record(tenantId, AuditEventType.TASK_CREATED, SecurityActorSupport.currentActorId(),
                "task", saved.getId().toString(), AuditActionResult.SUCCESS, request.getStatus().name());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public TaskResponse updateTask(UUID taskId, UpdateTaskRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();

        Task task = taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        TaskStatus previous = task.getStatus();
        task.setStatus(toEntityStatus(request.getStatus()));

        Task updated = taskRepository.save(task);
        auditService.record(tenantId, AuditEventType.TASK_STATUS_CHANGED, SecurityActorSupport.currentActorId(),
                "task", updated.getId().toString(), AuditActionResult.SUCCESS,
                previous.name() + " -> " + updated.getStatus().name());
        return toResponse(updated);
    }

    private TaskStatus toEntityStatus(TaskStatusDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Task status is required");
        }
        return TaskStatus.valueOf(dto.name());
    }

    private TaskStatusDto toDtoStatus(TaskStatus status) {
        return TaskStatusDto.valueOf(status.name());
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getProject().getId(),
                toDtoStatus(task.getStatus()),
                task.getTenantId(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
