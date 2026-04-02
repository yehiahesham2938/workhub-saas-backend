package com.workhub.saasbackend.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.workhub.saasbackend.dto.shared.UserRoleDto;

public record LoginResponse(
        String tokenType,
        String accessToken,
        Instant expiresAt,
        UUID userId,
        String email,
        String tenantId,
        UserRoleDto role
) {
}