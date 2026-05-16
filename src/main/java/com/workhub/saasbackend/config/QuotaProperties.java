package com.workhub.saasbackend.config;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.workhub.saasbackend.entity.TenantPlan;

@Component
@ConfigurationProperties(prefix = "workhub.quotas")
public class QuotaProperties {

    private Map<TenantPlan, PlanLimits> plans = defaultPlans();

    public Map<TenantPlan, PlanLimits> getPlans() {
        return plans;
    }

    public void setPlans(Map<TenantPlan, PlanLimits> plans) {
        this.plans = plans;
    }

    public PlanLimits limitsFor(TenantPlan plan) {
        return plans.getOrDefault(plan, plans.get(TenantPlan.FREE));
    }

    private static Map<TenantPlan, PlanLimits> defaultPlans() {
        Map<TenantPlan, PlanLimits> defaults = new EnumMap<>(TenantPlan.class);
        defaults.put(TenantPlan.FREE, new PlanLimits(2, 5, 3, 10));
        defaults.put(TenantPlan.STARTER, new PlanLimits(5, 20, 10, 50));
        defaults.put(TenantPlan.PRO, new PlanLimits(20, 100, 50, 200));
        defaults.put(TenantPlan.ENTERPRISE, new PlanLimits(100, 500, 200, 1000));
        return defaults;
    }

    public record PlanLimits(
            int maxWorkspaces,
            int maxProjects,
            int maxOpenJobs,
            int maxTasksPerProject
    ) {}
}
