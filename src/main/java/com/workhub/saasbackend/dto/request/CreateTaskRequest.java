package com.workhub.saasbackend.dto.request;

import java.util.UUID;

import com.workhub.saasbackend.dto.shared.TaskStatusDto;

import jakarta.validation.constraints.NotNull;

public class CreateTaskRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotNull(message = "Task status is required")
    private TaskStatusDto status;

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public TaskStatusDto getStatus() {
        return status;
    }

    public void setStatus(TaskStatusDto status) {
        this.status = status;
    }
}