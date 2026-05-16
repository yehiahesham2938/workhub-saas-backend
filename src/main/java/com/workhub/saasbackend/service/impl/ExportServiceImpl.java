package com.workhub.saasbackend.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workhub.saasbackend.dto.response.AuditEventResponse;
import com.workhub.saasbackend.dto.response.JobResponse;
import com.workhub.saasbackend.dto.response.PagedResponse;
import com.workhub.saasbackend.dto.response.ProjectResponse;
import com.workhub.saasbackend.dto.response.TenantSummaryResponse;
import com.workhub.saasbackend.entity.AuditEvent;
import com.workhub.saasbackend.entity.Job;
import com.workhub.saasbackend.entity.Project;
import com.workhub.saasbackend.feature.TenantFeature;
import com.workhub.saasbackend.feature.TenantFeatureService;
import com.workhub.saasbackend.repository.AuditEventRepository;
import com.workhub.saasbackend.repository.JobRepository;
import com.workhub.saasbackend.repository.ProjectRepository;
import com.workhub.saasbackend.security.TenantContext;
import com.workhub.saasbackend.service.ExportService;
import com.workhub.saasbackend.service.OperationalService;

@Service
public class ExportServiceImpl implements ExportService {

    private static final int MAX_PAGE_SIZE = 100;

    private final TenantFeatureService tenantFeatureService;
    private final AuditEventRepository auditEventRepository;
    private final JobRepository jobRepository;
    private final ProjectRepository projectRepository;
    private final OperationalService operationalService;

    public ExportServiceImpl(TenantFeatureService tenantFeatureService,
                             AuditEventRepository auditEventRepository,
                             JobRepository jobRepository,
                             ProjectRepository projectRepository,
                             OperationalService operationalService) {
        this.tenantFeatureService = tenantFeatureService;
        this.auditEventRepository = auditEventRepository;
        this.jobRepository = jobRepository;
        this.projectRepository = projectRepository;
        this.operationalService = operationalService;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AuditEventResponse> exportAudit(Pageable pageable) {
        tenantFeatureService.requireFeature(TenantFeature.EXPORT_REPORTS);
        String tenantId = TenantContext.getRequiredTenantId();
        Pageable bounded = bound(pageable);

        Page<AuditEvent> page = auditEventRepository.findAllByTenantIdOrderByOccurredAtDesc(tenantId, bounded);
        return mapAudit(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<JobResponse> exportJobs(Pageable pageable) {
        tenantFeatureService.requireFeature(TenantFeature.EXPORT_REPORTS);
        String tenantId = TenantContext.getRequiredTenantId();
        Pageable bounded = bound(pageable);

        Page<Job> page = jobRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId, bounded);
        return new PagedResponse<>(
                page.getContent().stream().map(this::toJobResponse).toList(),
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
    public PagedResponse<ProjectResponse> exportProjects(Pageable pageable) {
        tenantFeatureService.requireFeature(TenantFeature.EXPORT_REPORTS);
        String tenantId = TenantContext.getRequiredTenantId();
        Pageable bounded = bound(pageable);

        Page<Project> page = projectRepository.findAllByTenantId(tenantId, bounded);
        return new PagedResponse<>(
                page.getContent().stream().map(this::toProjectResponse).toList(),
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
    public TenantSummaryResponse exportUsageSummary() {
        tenantFeatureService.requireFeature(TenantFeature.EXPORT_REPORTS);
        return operationalService.getTenantSummary();
    }

    private Pageable bound(Pageable pageable) {
        int size = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        return Pageable.ofSize(size).withPage(pageable.getPageNumber());
    }

    private PagedResponse<AuditEventResponse> mapAudit(Page<AuditEvent> page) {
        return new PagedResponse<>(
                page.getContent().stream().map(event -> new AuditEventResponse(
                        event.getId(),
                        event.getTenantId(),
                        event.getEventType(),
                        event.getActorId(),
                        event.getResourceType(),
                        event.getResourceId(),
                        event.getActionResult(),
                        event.getDetails(),
                        event.getOccurredAt()
                )).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }

    private JobResponse toJobResponse(Job job) {
        return new JobResponse(
                job.getId(),
                job.getStatus(),
                job.getTenantId(),
                job.getErrorMessage(),
                job.getIdempotencyKey(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }

    private ProjectResponse toProjectResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getCreatedBy(),
                project.getTenantId(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
