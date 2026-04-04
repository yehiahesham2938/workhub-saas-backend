package com.workhub.saasbackend.dto.request;

import java.util.List;

import com.workhub.saasbackend.dto.shared.TaskStatusDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public class CreateProjectWithTasksRequest {

	@NotBlank(message = "Project name is required")
	@Size(max = 150, message = "Project name must be at most 150 characters")
	private String name;

	@NotEmpty(message = "At least one task status must be provided")
	private List<TaskStatusDto> taskStatuses;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<TaskStatusDto> getTaskStatuses() {
		return taskStatuses;
	}

	public void setTaskStatuses(List<TaskStatusDto> taskStatuses) {
		this.taskStatuses = taskStatuses;
	}
}

