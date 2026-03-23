package com.workhub.saasbackend.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class WorkspaceResponse {

    private UUID id;
    private String name;
    private String ownerEmail;
    private String tenantId;
    private Instant createdAt;
    private Instant updatedAt;
}
