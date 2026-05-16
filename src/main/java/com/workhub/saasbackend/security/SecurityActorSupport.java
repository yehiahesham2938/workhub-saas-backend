package com.workhub.saasbackend.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityActorSupport {

    private SecurityActorSupport() {
    }

    public static String currentActorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return null;
        }
        return authentication.getName();
    }

    public static UUID currentUserUuid() {
        String actorId = currentActorId();
        if (actorId == null) {
            throw new IllegalArgumentException("Authenticated user ID is missing");
        }
        try {
            return UUID.fromString(actorId);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Authenticated user ID must be a valid UUID");
        }
    }
}
