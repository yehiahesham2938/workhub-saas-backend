package com.workhub.saasbackend.service;

import java.util.UUID;

import com.workhub.saasbackend.dto.request.CreateTaskRequest;
import com.workhub.saasbackend.dto.request.UpdateTaskRequest;
import com.workhub.saasbackend.dto.response.TaskResponse;

public interface TaskService {

    TaskResponse createTask(UUID projectId, CreateTaskRequest request);

    TaskResponse updateTask(UUID taskId, UpdateTaskRequest request);
}
