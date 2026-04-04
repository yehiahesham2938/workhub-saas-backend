package com.workhub.saasbackend.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.workhub.saasbackend.dto.request.CreateProjectRequest;
import com.workhub.saasbackend.dto.response.PagedResponse;
import com.workhub.saasbackend.dto.response.ProjectResponse;
import com.workhub.saasbackend.dto.request.CreateProjectWithTasksRequest;

public interface ProjectService {

    ProjectResponse createProject(CreateProjectRequest request);

    PagedResponse<ProjectResponse> listProjects(Pageable pageable);

    ProjectResponse getProject(UUID id);

    void deleteProject(UUID id);

    /**
     * Creates a project and multiple tasks in a single transaction,
     * then throws an unchecked exception to demonstrate rollback.
     * Nothing should be persisted when this method finishes.
     */
    void createProjectWithTasksAndRollback(CreateProjectWithTasksRequest request);
}
