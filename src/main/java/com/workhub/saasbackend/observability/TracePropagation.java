package com.workhub.saasbackend.observability;

import com.workhub.saasbackend.messaging.JobMessage;

public final class TracePropagation {

    private TracePropagation() {
    }

    public static JobMessage enrich(JobMessage message) {
        if (message == null) {
            return null;
        }
        message.setTraceId(TraceContext.getTraceId());
        message.setSpanId(TraceContext.childSpanId());
        return message;
    }

    public static void restoreFromMessage(JobMessage message) {
        if (message == null || message.getTraceId() == null) {
            return;
        }
        TraceContext.set(message.getTraceId(), message.getSpanId() != null
                ? message.getSpanId()
                : TraceContext.childSpanId());
    }
}
