package com.workhub.saasbackend.dto.response;

import java.util.Set;

import com.workhub.saasbackend.entity.TenantPlan;
import com.workhub.saasbackend.feature.TenantFeature;

public record TenantConfigResponse(
        String tenantId,
        TenantPlan plan,
        Set<TenantFeature> enabledFeatures
) {}
