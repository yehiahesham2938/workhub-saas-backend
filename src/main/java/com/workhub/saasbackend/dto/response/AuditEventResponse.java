package com.workhub.saasbackend.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.workhub.saasbackend.entity.AuditActionResult;
import com.workhub.saasbackend.entity.AuditEventType;

public record AuditEventResponse(
        UUID id,
        String tenantId,
        AuditEventType eventType,
        String actorId,
        String resourceType,
        String resourceId,
        AuditActionResult actionResult,
        String details,
        Instant occurredAt
) {}
