package com.workhub.saasbackend.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class BusinessMetricsTest {

    @Test
    void recordJobCreated_incrementsCounter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BusinessMetrics metrics = new BusinessMetrics(registry);

        metrics.recordJobCreated("tenant-a");
        metrics.recordJobCreated("tenant-a");

        double count = registry.get("workhub.jobs.created")
                .tag("tenant", "tenant-a")
                .counter()
                .count();
        assertThat(count).isEqualTo(2.0);
    }

    @Test
    void recordCrossTenantDenied_incrementsSecurityCounter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BusinessMetrics metrics = new BusinessMetrics(registry);

        metrics.recordCrossTenantDenied("tenant-b", "project");

        assertThat(registry.get("workhub.security.cross_tenant.denied").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordQuotaExceeded_incrementsQuotaCounter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BusinessMetrics metrics = new BusinessMetrics(registry);

        metrics.recordQuotaExceeded("tenant-a", "workspace");

        assertThat(registry.get("workhub.quotas.exceeded").counter().count()).isEqualTo(1.0);
    }
}
