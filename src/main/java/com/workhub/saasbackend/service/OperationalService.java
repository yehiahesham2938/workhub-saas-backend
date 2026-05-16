package com.workhub.saasbackend.service;

import java.util.UUID;

import com.workhub.saasbackend.dto.response.DeadLetterQueueResponse;
import com.workhub.saasbackend.dto.response.QuotaUsageResponse;
import com.workhub.saasbackend.dto.response.TenantSummaryResponse;

public interface OperationalService {

    QuotaUsageResponse getQuotaUsage();

    TenantSummaryResponse getTenantSummary();

    DeadLetterQueueResponse inspectDeadLetterQueue();
}
