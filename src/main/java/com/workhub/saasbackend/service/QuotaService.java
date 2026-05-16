package com.workhub.saasbackend.service;

import java.util.UUID;

public interface QuotaService {

    void checkWorkspaceQuota(String tenantId);

    void checkProjectQuota(String tenantId);

    void checkOpenJobQuota(String tenantId);

    void checkTaskQuota(String tenantId, UUID projectId);
}
