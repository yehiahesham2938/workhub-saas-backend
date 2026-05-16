package com.workhub.saasbackend.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workhub.saasbackend.config.CacheConfig;
import com.workhub.saasbackend.dto.request.CreateProjectRequest;
import com.workhub.saasbackend.dto.request.CreateProjectWithTasksRequest;
import com.workhub.saasbackend.dto.request.ProvisionProjectSagaRequest;
import com.workhub.saasbackend.dto.response.PagedResponse;
import com.workhub.saasbackend.dto.response.ProjectResponse;
import com.workhub.saasbackend.dto.response.WorkflowExecutionResponse;
import com.workhub.saasbackend.dto.shared.TaskStatusDto;
import com.workhub.saasbackend.entity.AuditActionResult;
import com.workhub.saasbackend.entity.AuditEventType;
import com.workhub.saasbackend.entity.Project;
import com.workhub.saasbackend.entity.Task;
import com.workhub.saasbackend.entity.TaskStatus;
import com.workhub.saasbackend.entity.WorkflowExecution;
import com.workhub.saasbackend.entity.WorkflowStatus;
import com.workhub.saasbackend.exception.ResourceNotFoundException;
import com.workhub.saasbackend.repository.ProjectRepository;
import com.workhub.saasbackend.repository.TaskRepository;
import com.workhub.saasbackend.repository.WorkflowExecutionRepository;
import com.workhub.saasbackend.security.SecurityActorSupport;
import com.workhub.saasbackend.security.TenantContext;
import com.workhub.saasbackend.security.TenantResourceGuard;
import com.workhub.saasbackend.observability.BusinessLogger;
import com.workhub.saasbackend.observability.BusinessMetrics;
import com.workhub.saasbackend.service.AuditService;
import com.workhub.saasbackend.service.ProjectService;
import com.workhub.saasbackend.service.QuotaService;

import io.micrometer.core.instrument.Timer;

@Service
public class ProjectServiceImpl implements ProjectService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of("id", "name", "createdAt", "updatedAt");

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final AuditService auditService;
    private final QuotaService quotaService;
    private final TenantResourceGuard tenantResourceGuard;
    private final BusinessMetrics businessMetrics;

    public ProjectServiceImpl(ProjectRepository projectRepository,
                              TaskRepository taskRepository,
                              WorkflowExecutionRepository workflowExecutionRepository,
                              AuditService auditService,
                              QuotaService quotaService,
                              TenantResourceGuard tenantResourceGuard,
                              BusinessMetrics businessMetrics) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.workflowExecutionRepository = workflowExecutionRepository;
        this.auditService = auditService;
        this.quotaService = quotaService;
        this.tenantResourceGuard = tenantResourceGuard;
        this.businessMetrics = businessMetrics;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PROJECTS_CACHE, allEntries = true)
    public ProjectResponse createProject(CreateProjectRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();
        Timer.Sample sample = businessMetrics.startProjectCreateTimer();
        try {
            quotaService.checkProjectQuota(tenantId);

            Project project = new Project();
            project.setTenantId(tenantId);
            project.setName(request.getName());
            project.setCreatedBy(SecurityActorSupport.currentUserUuid());

            Project saved = projectRepository.save(project);
            businessMetrics.recordProjectCreated(tenantId);
            auditService.record(tenantId, AuditEventType.PROJECT_CREATED, SecurityActorSupport.currentActorId(),
                    "project", saved.getId().toString(), AuditActionResult.SUCCESS, request.getName());
            BusinessLogger.info("PROJECT_CREATED", "project persisted",
                    "projectId", saved.getId(), "tenantId", tenantId, "name", request.getName());
            return toResponse(saved);
        } finally {
            businessMetrics.recordProjectCreateDuration(sample, tenantId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProjectResponse> listProjects(Pageable pageable) {
        String tenantId = TenantContext.getRequiredTenantId();

        Pageable safePageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sanitizeSort(pageable.getSort())
        );

        Page<Project> page = projectRepository.findAllByTenantId(tenantId, safePageable);
        return new PagedResponse<>(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.PROJECTS_CACHE,
            key = "T(com.workhub.saasbackend.security.TenantContext).getRequiredTenantId() + ':' + #id")
    public ProjectResponse getProject(UUID id) {
        Project project = tenantResourceGuard.requireTenantResource(
                id,
                "project",
                projectRepository::findById,
                Project::getTenantId,
                () -> new ResourceNotFoundException("Project not found"));
        return toResponse(project);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.PROJECTS_CACHE, allEntries = true)
    public void deleteProject(UUID id) {
        String tenantId = TenantContext.getRequiredTenantId();
        Project project = projectRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        projectRepository.delete(project);
        auditService.record(tenantId, AuditEventType.PROJECT_DELETED, SecurityActorSupport.currentActorId(),
                "project", id.toString(), AuditActionResult.SUCCESS, null);
    }

    @Override
    @Transactional
    public void createProjectWithTasksAndRollback(CreateProjectWithTasksRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();
        quotaService.checkProjectQuota(tenantId);

        Project project = new Project();
        project.setTenantId(tenantId);
        project.setName(request.getName());
        project.setCreatedBy(SecurityActorSupport.currentUserUuid());
        Project savedProject = projectRepository.save(project);

        int index = 0;
        for (TaskStatusDto statusDto : request.getTaskStatuses()) {
            if (index == 1) {
                auditService.record(tenantId, AuditEventType.PROJECT_TX_ROLLBACK, SecurityActorSupport.currentActorId(),
                        "project", savedProject.getId().toString(), AuditActionResult.FAILURE,
                        "Simulated failure to demonstrate transaction rollback");
                throw new RuntimeException("Simulated failure to demonstrate transaction rollback");
            }
            Task task = new Task();
            task.setTenantId(tenantId);
            task.setProject(savedProject);
            task.setStatus(TaskStatus.valueOf(statusDto.name()));
            taskRepository.save(task);
            index++;
        }
    }

    @Override
    @Transactional
    public WorkflowExecutionResponse provisionProjectWithSaga(ProvisionProjectSagaRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();
        String actorId = SecurityActorSupport.currentActorId();
        quotaService.checkProjectQuota(tenantId);

        WorkflowExecution execution = new WorkflowExecution();
        execution.setTenantId(tenantId);
        execution.setWorkflowType("PROJECT_PROVISIONING");
        execution.setStatus(WorkflowStatus.RUNNING);
        execution.setCurrentStep("CREATE_PROJECT");
        workflowExecutionRepository.save(execution);

        auditService.record(tenantId, AuditEventType.PROJECT_SAGA_STARTED, actorId,
                "workflow", execution.getId().toString(), AuditActionResult.SUCCESS, request.getName());

        Project project = null;
        try {
            project = new Project();
            project.setTenantId(tenantId);
            project.setName(request.getName());
            project.setCreatedBy(SecurityActorSupport.currentUserUuid());
            project = projectRepository.save(project);
            execution.setResourceId(project.getId().toString());
            execution.setCurrentStep("CREATE_DEFAULT_TASKS");
            workflowExecutionRepository.save(execution);

            int index = 0;
            for (String statusName : request.getDefaultTaskStatuses()) {
                if (request.isSimulateFailure() && index == 1) {
                    throw new RuntimeException("Simulated saga failure at task creation");
                }
                quotaService.checkTaskQuota(tenantId, project.getId());
                Task task = new Task();
                task.setTenantId(tenantId);
                task.setProject(project);
                task.setStatus(TaskStatus.valueOf(statusName));
                taskRepository.save(task);
                index++;
            }

            execution.setStatus(WorkflowStatus.COMPLETED);
            execution.setCurrentStep("DONE");
            workflowExecutionRepository.save(execution);
            auditService.record(tenantId, AuditEventType.PROJECT_SAGA_COMPLETED, actorId,
                    "project", project.getId().toString(), AuditActionResult.SUCCESS, null);
            auditService.record(tenantId, AuditEventType.PROJECT_CREATED, actorId,
                    "project", project.getId().toString(), AuditActionResult.SUCCESS, request.getName());
            businessMetrics.recordSagaCompleted(tenantId, true);
            businessMetrics.recordProjectCreated(tenantId);
            BusinessLogger.info("SAGA_COMPLETED", "project provisioning saga completed",
                    "workflowId", execution.getId(), "projectId", project.getId(), "tenantId", tenantId);
            return toWorkflowResponse(execution);
        } catch (Exception ex) {
            compensateSaga(tenantId, project, execution, ex.getMessage(), actorId);
            return toWorkflowResponse(execution);
        }
    }

    private void compensateSaga(String tenantId, Project project, WorkflowExecution execution,
                                String reason, String actorId) {
        if (project != null && project.getId() != null) {
            taskRepository.deleteByTenantIdAndProjectId(tenantId, project.getId());
            projectRepository.delete(project);
        }
        execution.setStatus(WorkflowStatus.COMPENSATED);
        execution.setCurrentStep("COMPENSATED");
        execution.setFailureReason(reason);
        workflowExecutionRepository.save(execution);
        auditService.record(tenantId, AuditEventType.PROJECT_SAGA_COMPENSATED, actorId,
                "workflow", execution.getId().toString(), AuditActionResult.FAILURE, reason);
        businessMetrics.recordSagaCompleted(tenantId, false);
        BusinessLogger.warn("SAGA_COMPENSATED", "project provisioning saga compensated",
                "workflowId", execution.getId(), "tenantId", tenantId, "reason", reason);
    }

    private WorkflowExecutionResponse toWorkflowResponse(WorkflowExecution execution) {
        return new WorkflowExecutionResponse(
                execution.getId(),
                execution.getWorkflowType(),
                execution.getStatus(),
                execution.getCurrentStep(),
                execution.getResourceId(),
                execution.getFailureReason(),
                execution.getTenantId(),
                execution.getCreatedAt(),
                execution.getUpdatedAt()
        );
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getCreatedBy(),
                project.getTenantId(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    private Sort sanitizeSort(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        List<Sort.Order> allowedOrders = new ArrayList<>();
        for (Sort.Order order : sort) {
            if (ALLOWED_SORT_PROPERTIES.contains(order.getProperty())) {
                allowedOrders.add(order);
            }
        }

        if (allowedOrders.isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        return Sort.by(allowedOrders);
    }
}
