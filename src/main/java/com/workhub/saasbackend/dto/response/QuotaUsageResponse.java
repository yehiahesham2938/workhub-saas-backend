package com.workhub.saasbackend.dto.response;

import com.workhub.saasbackend.entity.TenantPlan;

public record QuotaUsageResponse(
        String tenantId,
        TenantPlan plan,
        UsageLimit workspaces,
        UsageLimit projects,
        UsageLimit openJobs,
        UsageLimit tasksPerProjectHint
) {
    public record UsageLimit(int used, int max, boolean exceeded) {}
}
