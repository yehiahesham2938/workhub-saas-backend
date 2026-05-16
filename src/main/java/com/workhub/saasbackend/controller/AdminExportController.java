package com.workhub.saasbackend.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workhub.saasbackend.dto.response.AuditEventResponse;
import com.workhub.saasbackend.dto.response.JobResponse;
import com.workhub.saasbackend.dto.response.PagedResponse;
import com.workhub.saasbackend.dto.response.ProjectResponse;
import com.workhub.saasbackend.dto.response.TenantSummaryResponse;
import com.workhub.saasbackend.service.ExportService;

/**
 * Read-only, tenant-scoped exports for management demos (Bonus 4). Requires EXPORT_REPORTS feature.
 */
@RestController
@RequestMapping("/admin/exports")
public class AdminExportController {

    private final ExportService exportService;

    public AdminExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/audit")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public PagedResponse<AuditEventResponse> exportAudit(
            @PageableDefault(size = 50, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return exportService.exportAudit(pageable);
    }

    @GetMapping("/jobs")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public PagedResponse<JobResponse> exportJobs(
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return exportService.exportJobs(pageable);
    }

    @GetMapping("/projects")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public PagedResponse<ProjectResponse> exportProjects(
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return exportService.exportProjects(pageable);
    }

    @GetMapping("/usage")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public TenantSummaryResponse exportUsage() {
        return exportService.exportUsageSummary();
    }
}
