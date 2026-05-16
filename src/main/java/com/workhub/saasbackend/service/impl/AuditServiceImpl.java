package com.workhub.saasbackend.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.workhub.saasbackend.dto.response.AuditEventResponse;
import com.workhub.saasbackend.dto.response.PagedResponse;
import com.workhub.saasbackend.entity.AuditActionResult;
import com.workhub.saasbackend.entity.AuditEvent;
import com.workhub.saasbackend.entity.AuditEventType;
import com.workhub.saasbackend.observability.BusinessLogger;
import com.workhub.saasbackend.observability.BusinessMetrics;
import com.workhub.saasbackend.repository.AuditEventRepository;
import com.workhub.saasbackend.security.TenantContext;
import com.workhub.saasbackend.service.AuditService;

@Service
public class AuditServiceImpl implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);

    private final AuditEventRepository auditEventRepository;
    private final BusinessMetrics businessMetrics;

    public AuditServiceImpl(AuditEventRepository auditEventRepository, BusinessMetrics businessMetrics) {
        this.auditEventRepository = auditEventRepository;
        this.businessMetrics = businessMetrics;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String tenantId, AuditEventType eventType, String actorId,
                       String resourceType, String resourceId, AuditActionResult result, String details) {
        AuditEvent event = new AuditEvent();
        event.setTenantId(tenantId);
        event.setEventType(eventType);
        event.setActorId(actorId);
        event.setResourceType(resourceType);
        event.setResourceId(resourceId);
        event.setActionResult(result);
        event.setDetails(details);
        auditEventRepository.save(event);
        businessMetrics.recordAuditEvent(tenantId, eventType.name());
        BusinessLogger.info("AUDIT_RECORDED", "audit event persisted",
                "eventType", eventType, "tenantId", tenantId, "resourceType", resourceType,
                "resourceId", resourceId, "result", result);
        log.debug("audit recorded type={} tenant={} resource={}/{}", eventType, tenantId, resourceType, resourceId);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AuditEventResponse> listForTenant(Pageable pageable) {
        String tenantId = TenantContext.getRequiredTenantId();
        Page<AuditEvent> page = auditEventRepository.findAllByTenantIdOrderByOccurredAtDesc(tenantId, pageable);
        return new PagedResponse<>(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }

    private AuditEventResponse toResponse(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getTenantId(),
                event.getEventType(),
                event.getActorId(),
                event.getResourceType(),
                event.getResourceId(),
                event.getActionResult(),
                event.getDetails(),
                event.getOccurredAt()
        );
    }
}
