package com.workhub.saasbackend.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.workhub.saasbackend.entity.JobStatus;

public record JobResponse(
	UUID id,
	JobStatus status,
	String tenantId,
	String errorMessage,
	Instant createdAt,
	Instant updatedAt
) {}

