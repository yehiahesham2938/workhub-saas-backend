package com.workhub.saasbackend.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        UUID createdBy,
        String tenantId,
        Instant createdAt,
        Instant updatedAt
) {
}
