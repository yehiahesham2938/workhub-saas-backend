package com.workhub.saasbackend.service;

import com.workhub.saasbackend.dto.response.DeadLetterReplayResponse;

public interface DeadLetterReplayService {

    /**
     * Admin support tool: drain up to {@code limit} messages from the DLQ for the current tenant only.
     * Skips jobs already {@code DONE} to avoid duplicate side effects.
     */
    DeadLetterReplayResponse replayForCurrentTenant(int limit);
}
