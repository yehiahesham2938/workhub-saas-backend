package com.workhub.saasbackend.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class TenantIsolationJobIT extends AbstractTenantIsolationIT {

    @Test
    void getJob_crossTenantReturnsNotFound() throws Exception {
        String jobAId = createJob(tokenAAdmin);

        mockMvc.perform(get("/jobs/" + jobAId)
                        .header("Authorization", bearer(tokenBAdmin)))
                .andExpect(status().isNotFound());
    }
}
