package com.workhub.saasbackend.dto.response;

import java.util.List;

public record DeadLetterReplayResponse(
        int requestedLimit,
        int replayed,
        int skipped,
        List<String> details
) {}
