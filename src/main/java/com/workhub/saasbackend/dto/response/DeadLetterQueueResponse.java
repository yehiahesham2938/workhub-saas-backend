package com.workhub.saasbackend.dto.response;

public record DeadLetterQueueResponse(
        String queueName,
        int messageCount,
        String jobsQueueName,
        int jobsQueueDepth,
        String description
) {}
