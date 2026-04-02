package com.workhub.saasbackend.dto.request;

import com.workhub.saasbackend.dto.shared.TaskStatusDto;

import jakarta.validation.constraints.NotNull;

public class UpdateTaskRequest {

    @NotNull(message = "Task status is required")
    private TaskStatusDto status;

    public TaskStatusDto getStatus() {
        return status;
    }

    public void setStatus(TaskStatusDto status) {
        this.status = status;
    }
}