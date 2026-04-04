package com.workhub.saasbackend.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workhub.saasbackend.dto.request.CreateTaskRequest;
import com.workhub.saasbackend.dto.request.UpdateTaskRequest;
import com.workhub.saasbackend.dto.response.TaskResponse;
import com.workhub.saasbackend.dto.shared.TaskStatusDto;
import com.workhub.saasbackend.entity.Project;
import com.workhub.saasbackend.entity.Task;
import com.workhub.saasbackend.entity.TaskStatus;
import com.workhub.saasbackend.exception.ResourceNotFoundException;
import com.workhub.saasbackend.repository.ProjectRepository;
import com.workhub.saasbackend.repository.TaskRepository;
import com.workhub.saasbackend.security.TenantContext;
import com.workhub.saasbackend.service.TaskService;
import org.springframework.security.access.AccessDeniedException;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    public TaskServiceImpl(TaskRepository taskRepository, ProjectRepository projectRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
    }

    @Override
    @Transactional
    public TaskResponse createTask(UUID projectId, CreateTaskRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        if (!tenantId.equals(project.getTenantId())) {
            throw new AccessDeniedException("Access denied: tenant mismatch");
        }

        Task task = new Task();
        task.setTenantId(tenantId);
        task.setProject(project);
        task.setStatus(toEntityStatus(request.getStatus()));

        Task saved = taskRepository.save(task);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public TaskResponse updateTask(UUID taskId, UpdateTaskRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        if (!tenantId.equals(task.getTenantId())) {
            throw new AccessDeniedException("Access denied: tenant mismatch");
        }

        task.setStatus(toEntityStatus(request.getStatus()));

        Task updated = taskRepository.save(task);
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
