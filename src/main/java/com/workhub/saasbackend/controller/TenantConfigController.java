package com.workhub.saasbackend.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workhub.saasbackend.dto.response.TenantConfigResponse;
import com.workhub.saasbackend.security.TenantContext;
import com.workhub.saasbackend.service.TenantConfigService;

@RestController
@RequestMapping("/api/v1/tenant")
public class TenantConfigController {

    private final TenantConfigService tenantConfigService;

    public TenantConfigController(TenantConfigService tenantConfigService) {
        this.tenantConfigService = tenantConfigService;
    }

    /** Cached tenant plan and enabled feature flags (Bonus 2 + 7). */
    @GetMapping("/config")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    public TenantConfigResponse config() {
        return tenantConfigService.getTenantConfig(TenantContext.getRequiredTenantId());
    }
}
