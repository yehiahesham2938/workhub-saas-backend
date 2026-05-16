package com.workhub.saasbackend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import io.micrometer.core.instrument.MeterRegistry;

class Phase3ObservabilityIT extends AbstractTenantIsolationIT {

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void httpRequest_shouldReturnCorrelationIdHeader() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", bearer(tokenAAdmin)))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void projectCreate_shouldIncrementBusinessMetrics() throws Exception {
        double before = counterValue("workhub.projects.created", "tenant-a");

        createProject(tokenAAdmin, "Metrics Project " + UUID.randomUUID());

        double after = counterValue("workhub.projects.created", "tenant-a");
        assertThat(after).isGreaterThan(before);
    }

    @Test
    void crossTenantDenied_shouldIncrementSecurityMetric() throws Exception {
        String projectId = createProject(tokenAAdmin, "Metrics Cross Tenant");
        double before = counterValue("workhub.security.cross_tenant.denied", "tenant-b");

        mockMvc.perform(get("/projects/" + projectId)
                        .header("Authorization", bearer(tokenBAdmin))
                        .header("X-Correlation-Id", "cid-metrics-test"))
                .andExpect(status().isNotFound());

        double after = counterValue("workhub.security.cross_tenant.denied", "tenant-b");
        assertThat(after).isGreaterThan(before);
    }

    @Test
    void actuatorMetricsEndpoint_shouldBeAccessible() throws Exception {
        mockMvc.perform(get("/actuator/metrics/workhub.projects.created"))
                .andExpect(status().isOk());
    }

    private double counterValue(String name, String tenant) {
        var counter = meterRegistry.find(name).tag("tenant", tenant).counter();
        return counter != null ? counter.count() : 0.0;
    }
}
