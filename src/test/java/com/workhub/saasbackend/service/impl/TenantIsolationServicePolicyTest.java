package com.workhub.saasbackend.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.workhub.saasbackend.dto.request.CreateTaskRequest;
import com.workhub.saasbackend.dto.request.UpdateTaskRequest;
import com.workhub.saasbackend.dto.shared.TaskStatusDto;
import com.workhub.saasbackend.entity.Job;
import com.workhub.saasbackend.entity.JobStatus;
import com.workhub.saasbackend.entity.Project;
import com.workhub.saasbackend.entity.Task;
import com.workhub.saasbackend.entity.TaskStatus;
import com.workhub.saasbackend.entity.Workspace;
import com.workhub.saasbackend.exception.ResourceNotFoundException;
import com.workhub.saasbackend.messaging.JobProducer;
import com.workhub.saasbackend.repository.JobRepository;
import com.workhub.saasbackend.repository.ProjectRepository;
import com.workhub.saasbackend.repository.TaskRepository;
import com.workhub.saasbackend.repository.WorkspaceRepository;
import com.workhub.saasbackend.security.TenantContext;

@ExtendWith(MockitoExtension.class)
class TenantIsolationServicePolicyTest {

    private static final String TENANT_A = "tenantA";
    private static final String TENANT_B = "tenantB";

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobProducer jobProducer;

    @InjectMocks
    private ProjectServiceImpl projectService;

    @InjectMocks
    private TaskServiceImpl taskService;

    @InjectMocks
    private WorkspaceServiceImpl workspaceService;

    @InjectMocks
    private JobServiceImpl jobService;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void projectList_isTenantFiltered() {
        TenantContext.setTenantId(TENANT_B);
        Pageable pageable = PageRequest.of(0, 20);

        when(projectRepository.findAllByTenantId(eq(TENANT_B), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertDoesNotThrow(() -> projectService.listProjects(pageable));
        verify(projectRepository).findAllByTenantId(eq(TENANT_B), any(Pageable.class));
    }

    @Test
    void projectGet_crossTenantIdReturnsNotFound() {
        UUID projectId = UUID.randomUUID();
        TenantContext.setTenantId(TENANT_B);
        when(projectRepository.findByIdAndTenantId(projectId, TENANT_B)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projectService.getProject(projectId));
    }

    @Test
    void projectDelete_crossTenantIdReturnsNotFound() {
        UUID projectId = UUID.randomUUID();
        TenantContext.setTenantId(TENANT_B);
        when(projectRepository.findByIdAndTenantId(projectId, TENANT_B)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projectService.deleteProject(projectId));
    }

    @Test
    void createChildTask_underForeignTenantProjectReturnsNotFound() {
        UUID projectId = UUID.randomUUID();
        CreateTaskRequest request = new CreateTaskRequest();
        request.setStatus(TaskStatusDto.TODO);
        TenantContext.setTenantId(TENANT_B);
        when(projectRepository.findByIdAndTenantId(projectId, TENANT_B)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.createTask(projectId, request));
    }

    @Test
    void taskUpdate_crossTenantIdReturnsNotFound() {
        UUID taskId = UUID.randomUUID();
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setStatus(TaskStatusDto.DONE);
        TenantContext.setTenantId(TENANT_B);
        when(taskRepository.findByIdAndTenantId(taskId, TENANT_B)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.updateTask(taskId, request));
    }

    @Test
    void workspaceList_isTenantFiltered() {
        TenantContext.setTenantId(TENANT_B);
        when(workspaceRepository.findAllByTenantId(TENANT_B)).thenReturn(List.of());

        assertEquals(0, workspaceService.listWorkspaces().size());
        verify(workspaceRepository).findAllByTenantId(TENANT_B);
    }

    @Test
    void workspaceGet_crossTenantIdReturnsNotFound() {
        UUID workspaceId = UUID.randomUUID();
        TenantContext.setTenantId(TENANT_B);
        when(workspaceRepository.findByIdAndTenantId(workspaceId, TENANT_B)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> workspaceService.getWorkspace(workspaceId));
    }

    @Test
    void jobGet_crossTenantIdReturnsNotFound() {
        UUID jobId = UUID.randomUUID();
        TenantContext.setTenantId(TENANT_B);
        when(jobRepository.findByIdAndTenantId(jobId, TENANT_B)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> jobService.getJob(jobId));
    }

    @Test
    void taskUpdate_sameTenantSucceeds() {
        UUID taskId = UUID.randomUUID();
        TenantContext.setTenantId(TENANT_A);

        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setTenantId(TENANT_A);
        project.setName("A Project");
        project.setCreatedBy(UUID.randomUUID());
        project.setCreatedAt(Instant.now());
        project.setUpdatedAt(Instant.now());

        Task task = new Task();
        task.setId(taskId);
        task.setTenantId(TENANT_A);
        task.setProject(project);
        task.setStatus(TaskStatus.TODO);
        task.setCreatedAt(Instant.now());
        task.setUpdatedAt(Instant.now());

        when(taskRepository.findByIdAndTenantId(taskId, TENANT_A)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setStatus(TaskStatusDto.IN_PROGRESS);
        taskService.updateTask(taskId, request);

        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
    }

    @Test
    void jobGet_sameTenantSucceeds() {
        UUID jobId = UUID.randomUUID();
        TenantContext.setTenantId(TENANT_A);

        Job job = new Job();
        job.setId(jobId);
        job.setTenantId(TENANT_A);
        job.setStatus(JobStatus.PENDING);
        job.setCreatedAt(Instant.now());
        job.setUpdatedAt(Instant.now());

        when(jobRepository.findByIdAndTenantId(jobId, TENANT_A)).thenReturn(Optional.of(job));

        assertEquals(jobId, jobService.getJob(jobId).id());
    }
}
