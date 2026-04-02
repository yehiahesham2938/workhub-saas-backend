package com.workhub.saasbackend.controller;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.workhub.saasbackend.dto.request.CreateProjectRequest;
import com.workhub.saasbackend.dto.request.CreateTaskRequest;
import com.workhub.saasbackend.dto.response.PagedResponse;
import com.workhub.saasbackend.dto.response.ProjectResponse;
import com.workhub.saasbackend.dto.response.TaskResponse;
import com.workhub.saasbackend.service.ProjectService;
import com.workhub.saasbackend.service.TaskService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/projects")
@Validated
public class ProjectController {

    private final ProjectService projectService;
    private final TaskService taskService;

    public ProjectController(ProjectService projectService, TaskService taskService) {
        this.projectService = projectService;
        this.taskService = taskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(@Valid @RequestBody CreateProjectRequest request) {
        return projectService.createProject(request);
    }

    @GetMapping
    public PagedResponse<ProjectResponse> listProjects(@PageableDefault(size = 20) Pageable pageable) {
        return projectService.listProjects(pageable);
    }

    @GetMapping("/{id}")
    public ProjectResponse getProject(@PathVariable UUID id) {
        return projectService.getProject(id);
    }

    @PostMapping("/{id}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(@PathVariable UUID id, @Valid @RequestBody CreateTaskRequest request) {
        return taskService.createTask(id, request);
    }
}
