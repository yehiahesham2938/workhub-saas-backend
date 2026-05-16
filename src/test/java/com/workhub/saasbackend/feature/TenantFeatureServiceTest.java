package com.workhub.saasbackend.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.workhub.saasbackend.config.TenantPlanResolver;
import com.workhub.saasbackend.entity.TenantPlan;
import com.workhub.saasbackend.exception.FeatureNotEnabledException;
import com.workhub.saasbackend.security.TenantContext;

@ExtendWith(MockitoExtension.class)
class TenantFeatureServiceTest {

    @Mock
    private TenantPlanResolver tenantPlanResolver;

    @InjectMocks
    private TenantFeatureService tenantFeatureService;

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void proPlan_enablesDlqReplay() {
        TenantContext.setTenantId("tenant-b");
        when(tenantPlanResolver.resolvePlan("tenant-b")).thenReturn(TenantPlan.PRO);

        assertThat(tenantFeatureService.isEnabled(TenantFeature.DLQ_REPLAY)).isTrue();
    }

    @Test
    void freePlan_blocksExports() {
        TenantContext.setTenantId("tenant-default");
        when(tenantPlanResolver.resolvePlan("tenant-default")).thenReturn(TenantPlan.FREE);

        assertThatThrownBy(() -> tenantFeatureService.requireFeature(TenantFeature.EXPORT_REPORTS))
                .isInstanceOf(FeatureNotEnabledException.class);
    }
}
