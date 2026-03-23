package com.workhub.saasbackend.service;

import com.workhub.saasbackend.dto.request.CreateWorkspaceRequest;
import com.workhub.saasbackend.dto.response.WorkspaceResponse;
import java.util.List;
import java.util.UUID;

public interface WorkspaceService {

    WorkspaceResponse createWorkspace(CreateWorkspaceRequest request);

    WorkspaceResponse getWorkspace(UUID id);

    List<WorkspaceResponse> listWorkspaces();
}
