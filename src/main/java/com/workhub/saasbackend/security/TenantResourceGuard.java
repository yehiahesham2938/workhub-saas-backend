package com.workhub.saasbackend.security;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.workhub.saasbackend.entity.AuditActionResult;
import com.workhub.saasbackend.entity.AuditEventType;
import com.workhub.saasbackend.exception.ResourceNotFoundException;
import com.workhub.saasbackend.observability.BusinessLogger;
import com.workhub.saasbackend.observability.BusinessMetrics;
import com.workhub.saasbackend.service.AuditService;

@Component
public class TenantResourceGuard {

    private final AuditService auditService;
    private final BusinessMetrics businessMetrics;

    public TenantResourceGuard(AuditService auditService, BusinessMetrics businessMetrics) {
        this.auditService = auditService;
        this.businessMetrics = businessMetrics;
    }

    public <T> T requireTenantResource(
            UUID id,
            String resourceType,
            Function<UUID, Optional<T>> findById,
            Function<T, String> tenantIdExtractor,
            Supplier<ResourceNotFoundException> notFound) {
        String requestTenantId = TenantContext.getRequiredTenantId();
        Optional<T> scoped = findById.apply(id).filter(entity ->
                requestTenantId.equals(tenantIdExtractor.apply(entity)));

        if (scoped.isPresent()) {
            return scoped.get();
        }

        findById.apply(id).ifPresent(entity -> {
            String actualTenant = tenantIdExtractor.apply(entity);
            if (!requestTenantId.equals(actualTenant)) {
                auditService.record(requestTenantId, AuditEventType.CROSS_TENANT_DENIED,
                        SecurityActorSupport.currentActorId(),
                        resourceType, id.toString(), AuditActionResult.DENIED,
                        "attempted access to tenant " + actualTenant);
                businessMetrics.recordCrossTenantDenied(requestTenantId, resourceType);
                BusinessLogger.warn("CROSS_TENANT_DENIED", "cross-tenant access blocked",
                        "requestTenant", requestTenantId, "actualTenant", actualTenant,
                        "resourceType", resourceType, "resourceId", id);
            }
        });

        throw notFound.get();
    }
}
