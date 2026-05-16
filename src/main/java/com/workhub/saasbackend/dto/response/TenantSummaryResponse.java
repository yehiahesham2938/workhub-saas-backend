package com.workhub.saasbackend.dto.response;

import java.util.Map;

import com.workhub.saasbackend.entity.TenantPlan;

public record TenantSummaryResponse(
        String tenantId,
        TenantPlan plan,
        long workspaceCount,
        long projectCount,
        Map<String, Long> jobCountsByStatus,
        long auditEventCount,
        long workflowExecutionCount
) {}
