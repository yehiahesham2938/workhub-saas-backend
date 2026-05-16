package com.workhub.saasbackend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;

import com.workhub.saasbackend.config.CacheConfig;
import com.workhub.saasbackend.observability.TraceContext;

class Phase3BonusIT extends AbstractTenantIsolationIT {

    @Autowired
    private CacheManager cacheManager;

    @Test
    void distributedTrace_shouldEchoTraceHeaders() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", bearer(tokenAAdmin))
                        .header(TraceContext.TRACE_ID_HEADER, "abc123traceabc123traceabc123traceab"))
                .andExpect(status().isOk())
                .andExpect(header().exists(TraceContext.TRACE_ID_HEADER))
                .andExpect(header().exists(TraceContext.TRACEPARENT_HEADER));
    }

    @Test
    void tenantConfig_shouldBeCachedPerTenant() throws Exception {
        mockMvc.perform(get("/api/v1/tenant/config")
                        .header("Authorization", bearer(tokenAAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("tenant-a"))
                .andExpect(jsonPath("$.plan").value("STARTER"));

        var cache = cacheManager.getCache(CacheConfig.TENANT_CONFIG_CACHE);
        assertThat(cache).isNotNull();
        assertThat(cache.get("tenant-a")).isNotNull();
    }

    @Test
    void exportAudit_shouldRequireStarterPlanOrHigher() throws Exception {
        mockMvc.perform(get("/admin/exports/audit")
                        .header("Authorization", bearer(tokenAAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void dlqReplay_shouldBeForbiddenOnStarterPlan() throws Exception {
        mockMvc.perform(post("/admin/queues/dead-letter/replay?limit=1")
                        .header("Authorization", bearer(tokenAAdmin)))
                .andExpect(status().isForbidden());
    }

    @Test
    void dlqReplayEndpoint_shouldBeAvailableOnProPlan() throws Exception {
        mockMvc.perform(post("/admin/queues/dead-letter/replay?limit=1")
                        .header("Authorization", bearer(tokenBAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedLimit").value(1));
    }

    @Test
    void freeTierTenant_shouldHaveNoPremiumFeatures() throws Exception {
        String token = loginAndGetToken("user@other.com");

        mockMvc.perform(get("/api/v1/tenant/config")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("FREE"))
                .andExpect(jsonPath("$.enabledFeatures").isEmpty());
    }
}
