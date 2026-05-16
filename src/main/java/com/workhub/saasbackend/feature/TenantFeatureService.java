package com.workhub.saasbackend.feature;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.workhub.saasbackend.config.TenantPlanResolver;
import com.workhub.saasbackend.entity.TenantPlan;
import com.workhub.saasbackend.exception.FeatureNotEnabledException;
import com.workhub.saasbackend.security.TenantContext;

@Service
public class TenantFeatureService {

    private final TenantPlanResolver tenantPlanResolver;

    public TenantFeatureService(TenantPlanResolver tenantPlanResolver) {
        this.tenantPlanResolver = tenantPlanResolver;
    }

    public boolean isEnabled(TenantFeature feature) {
        String tenantId = TenantContext.getRequiredTenantId();
        TenantPlan plan = tenantPlanResolver.resolvePlan(tenantId);
        return featuresFor(plan).contains(feature);
    }

    public void requireFeature(TenantFeature feature) {
        if (!isEnabled(feature)) {
            TenantPlan plan = tenantPlanResolver.resolvePlan(TenantContext.getRequiredTenantId());
            throw new FeatureNotEnabledException(
                    "Feature " + feature.name() + " is not available on plan " + plan.name());
        }
    }

    public Set<TenantFeature> enabledFeatures() {
        String tenantId = TenantContext.getRequiredTenantId();
        return featuresForPlan(tenantPlanResolver.resolvePlan(tenantId));
    }

    public Set<TenantFeature> featuresForPlan(TenantPlan plan) {
        return featuresFor(plan);
    }

    private Set<TenantFeature> featuresFor(TenantPlan plan) {
        return switch (plan) {
            case FREE -> EnumSet.noneOf(TenantFeature.class);
            case STARTER -> EnumSet.of(TenantFeature.EXPORT_REPORTS, TenantFeature.SAGA_PROVISIONING);
            case PRO -> EnumSet.of(
                    TenantFeature.SAGA_PROVISIONING,
                    TenantFeature.EXPORT_REPORTS,
                    TenantFeature.DLQ_REPLAY,
                    TenantFeature.ADVANCED_OBSERVABILITY
            );
            case ENTERPRISE -> EnumSet.allOf(TenantFeature.class);
        };
    }
}
