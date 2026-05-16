package com.workhub.saasbackend.service;

import org.springframework.data.domain.Pageable;

import com.workhub.saasbackend.dto.response.AuditEventResponse;
import com.workhub.saasbackend.dto.response.PagedResponse;
import com.workhub.saasbackend.entity.AuditActionResult;
import com.workhub.saasbackend.entity.AuditEventType;

public interface AuditService {

    void record(String tenantId, AuditEventType eventType, String actorId,
                String resourceType, String resourceId, AuditActionResult result, String details);

    PagedResponse<AuditEventResponse> listForTenant(Pageable pageable);
}
