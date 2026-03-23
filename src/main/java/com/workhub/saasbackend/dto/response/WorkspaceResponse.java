package com.workhub.saasbackend.dto.response;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String name,
        String ownerEmail,
        String tenantId,
        Instant createdAt,
        Instant updatedAt
) {
}
