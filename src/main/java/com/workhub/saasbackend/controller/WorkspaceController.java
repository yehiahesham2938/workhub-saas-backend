package com.workhub.saasbackend.controller;

import com.workhub.saasbackend.dto.request.CreateWorkspaceRequest;
import com.workhub.saasbackend.dto.response.WorkspaceResponse;
import com.workhub.saasbackend.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceResponse createWorkspace(@Valid @RequestBody CreateWorkspaceRequest request) {
        return workspaceService.createWorkspace(request);
    }

    @GetMapping("/{id}")
    public WorkspaceResponse getWorkspace(@PathVariable UUID id) {
        return workspaceService.getWorkspace(id);
    }

    @GetMapping
    public List<WorkspaceResponse> listWorkspaces() {
        return workspaceService.listWorkspaces();
    }
}
