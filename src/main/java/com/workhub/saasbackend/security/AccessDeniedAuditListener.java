package com.workhub.saasbackend.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.access.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;

import com.workhub.saasbackend.entity.AuditActionResult;
import com.workhub.saasbackend.entity.AuditEventType;
import com.workhub.saasbackend.service.AuditService;

@Component
public class AccessDeniedAuditListener {

    private final AuditService auditService;

    public AccessDeniedAuditListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @EventListener
    public void onAuthorizationDenied(AuthorizationDeniedEvent<?> event) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            tenantId = "unknown";
        }
        auditService.record(tenantId, AuditEventType.AUTH_ACCESS_DENIED,
                SecurityActorSupport.currentActorId(),
                "endpoint", null, AuditActionResult.DENIED,
                event.getAuthorizationDecision().toString());
    }
}
