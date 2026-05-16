package com.workhub.saasbackend.config;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PROJECTS_CACHE = "projects";
    public static final String WORKSPACES_CACHE = "workspaces";
    public static final String TENANT_PLANS_CACHE = "tenantPlans";
    public static final String TENANT_CONFIG_CACHE = "tenantConfig";
    public static final String TENANT_DASHBOARD_CACHE = "tenantDashboard";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                PROJECTS_CACHE,
                WORKSPACES_CACHE,
                TENANT_PLANS_CACHE,
                TENANT_CONFIG_CACHE,
                TENANT_DASHBOARD_CACHE
        );
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500));
        return manager;
    }
}
