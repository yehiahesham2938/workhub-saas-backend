package com.workhub.saasbackend.service.impl;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.workhub.saasbackend.config.CacheConfig;
import com.workhub.saasbackend.config.TenantPlanResolver;
import com.workhub.saasbackend.dto.response.TenantConfigResponse;
import com.workhub.saasbackend.entity.TenantPlan;
import com.workhub.saasbackend.feature.TenantFeatureService;
import com.workhub.saasbackend.service.TenantConfigService;

@Service
public class TenantConfigServiceImpl implements TenantConfigService {

    private final TenantPlanResolver tenantPlanResolver;
    private final TenantFeatureService tenantFeatureService;

    public TenantConfigServiceImpl(TenantPlanResolver tenantPlanResolver,
                                   TenantFeatureService tenantFeatureService) {
        this.tenantPlanResolver = tenantPlanResolver;
        this.tenantFeatureService = tenantFeatureService;
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.TENANT_CONFIG_CACHE, key = "#tenantId")
    public TenantConfigResponse getTenantConfig(String tenantId) {
        TenantPlan plan = tenantPlanResolver.resolvePlan(tenantId);
        return new TenantConfigResponse(tenantId, plan, tenantFeatureService.featuresForPlan(plan));
    }
}
