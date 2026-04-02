package com.workhub.saasbackend.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.workhub.saasbackend.dto.shared.TaskStatusDto;

public record TaskResponse(
        UUID id,
        UUID projectId,
        TaskStatusDto status,
        String tenantId,
        Instant createdAt,
        Instant updatedAt
) {
}
