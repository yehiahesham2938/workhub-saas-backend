package com.workhub.saasbackend.service;

import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.workhub.saasbackend.dto.request.CreateProjectRequest;
import com.workhub.saasbackend.dto.response.PagedResponse;
import com.workhub.saasbackend.dto.response.ProjectResponse;

public interface ProjectService {

    ProjectResponse createProject(CreateProjectRequest request);

    PagedResponse<ProjectResponse> listProjects(Pageable pageable);

    ProjectResponse getProject(UUID id);
}
