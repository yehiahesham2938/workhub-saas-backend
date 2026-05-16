package com.workhub.saasbackend.feature;

/**
 * Tier-gated capabilities (Bonus 7). Enabled per {@link com.workhub.saasbackend.entity.TenantPlan}.
 */
public enum TenantFeature {
    SAGA_PROVISIONING,
    EXPORT_REPORTS,
    DLQ_REPLAY,
    ADVANCED_OBSERVABILITY
}
