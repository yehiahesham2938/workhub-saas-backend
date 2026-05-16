package com.workhub.saasbackend.service;

import org.springframework.data.domain.Pageable;

import com.workhub.saasbackend.dto.response.AuditEventResponse;
import com.workhub.saasbackend.dto.response.JobResponse;
import com.workhub.saasbackend.dto.response.PagedResponse;
import com.workhub.saasbackend.dto.response.ProjectResponse;
import com.workhub.saasbackend.dto.response.TenantSummaryResponse;

public interface ExportService {

    PagedResponse<AuditEventResponse> exportAudit(Pageable pageable);

    PagedResponse<JobResponse> exportJobs(Pageable pageable);

    PagedResponse<ProjectResponse> exportProjects(Pageable pageable);

    TenantSummaryResponse exportUsageSummary();
}
