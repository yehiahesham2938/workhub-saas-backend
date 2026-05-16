package com.workhub.saasbackend.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.workhub.saasbackend.dto.response.DeadLetterQueueResponse;
import com.workhub.saasbackend.dto.response.DeadLetterReplayResponse;
import com.workhub.saasbackend.dto.response.JobResponse;
import com.workhub.saasbackend.dto.response.QuotaUsageResponse;
import com.workhub.saasbackend.dto.response.TenantSummaryResponse;
import com.workhub.saasbackend.service.DeadLetterReplayService;
import com.workhub.saasbackend.service.JobService;
import com.workhub.saasbackend.service.OperationalService;

/**
 * Operational endpoints for support and marking demos.
 * All routes are admin-only and tenant-scoped via JWT tenant context.
 */
@RestController
@RequestMapping("/admin")
public class AdminOperationsController {

    private final OperationalService operationalService;
    private final JobService jobService;
    private final DeadLetterReplayService deadLetterReplayService;

    public AdminOperationsController(OperationalService operationalService,
                                     JobService jobService,
                                     DeadLetterReplayService deadLetterReplayService) {
        this.operationalService = operationalService;
        this.jobService = jobService;
        this.deadLetterReplayService = deadLetterReplayService;
    }

    /** Current tenant plan limits vs usage (workspaces, projects, open jobs). */
    @GetMapping("/quotas")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public QuotaUsageResponse quotaUsage() {
        return operationalService.getQuotaUsage();
    }

    /** Read-only snapshot for support: counts per resource type in the current tenant. */
    @GetMapping("/tenant/summary")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public TenantSummaryResponse tenantSummary() {
        return operationalService.getTenantSummary();
    }

    /** Inspect RabbitMQ job queue and dead-letter queue depths. */
    @GetMapping("/queues/dead-letter")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public DeadLetterQueueResponse deadLetterQueue() {
        return operationalService.inspectDeadLetterQueue();
    }

    /** Re-publish a FAILED or PENDING job for the current tenant (idempotent consumer safe). */
    @PostMapping("/jobs/{id}/retry")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public JobResponse retryJob(@PathVariable UUID id) {
        return jobService.retryJob(id);
    }

    /**
     * Bonus 3: replay up to {@code limit} DLQ messages for the current tenant only.
     * Skips terminal jobs; audited as admin action.
     */
    @PostMapping("/queues/dead-letter/replay")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public DeadLetterReplayResponse replayDeadLetterQueue(
            @RequestParam(defaultValue = "10") int limit) {
        return deadLetterReplayService.replayForCurrentTenant(limit);
    }
}
