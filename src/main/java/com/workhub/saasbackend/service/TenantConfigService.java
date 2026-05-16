package com.workhub.saasbackend.service;

import com.workhub.saasbackend.dto.response.TenantConfigResponse;

public interface TenantConfigService {

    TenantConfigResponse getTenantConfig(String tenantId);
}
