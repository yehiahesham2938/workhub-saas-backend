package com.workhub.saasbackend.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.workhub.saasbackend.entity.WorkflowStatus;

public record WorkflowExecutionResponse(
        UUID id,
        String workflowType,
        WorkflowStatus status,
        String currentStep,
        String resourceId,
        String failureReason,
        String tenantId,
        Instant createdAt,
        Instant updatedAt
) {}
