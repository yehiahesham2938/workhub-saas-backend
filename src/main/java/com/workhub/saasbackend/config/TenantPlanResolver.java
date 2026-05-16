package com.workhub.saasbackend.config;

import org.springframework.stereotype.Component;

import com.workhub.saasbackend.entity.TenantPlan;

@Component
public class TenantPlanResolver {

    public TenantPlan resolvePlan(String tenantId) {
        if (tenantId == null) {
            return TenantPlan.FREE;
        }
        return switch (tenantId) {
            case "tenant-a" -> TenantPlan.STARTER;
            case "tenant-b" -> TenantPlan.PRO;
            default -> TenantPlan.FREE;
        };
    }
}
