package com.workhub.saasbackend.observability;

import org.slf4j.MDC;

/**
 * Thread-local distributed trace state (W3C-inspired). Propagated to async job messages.
 */
public final class TraceContext {

    public static final String TRACE_ID_KEY = "traceId";
    public static final String SPAN_ID_KEY = "spanId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String SPAN_ID_HEADER = "X-Span-Id";
    public static final String TRACEPARENT_HEADER = "traceparent";

    private static final ThreadLocal<String> CURRENT_TRACE_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_SPAN_ID = new ThreadLocal<>();

    private TraceContext() {
    }

    public static void set(String traceId, String spanId) {
        CURRENT_TRACE_ID.set(traceId);
        CURRENT_SPAN_ID.set(spanId);
        if (traceId != null) {
            MDC.put(TRACE_ID_KEY, traceId);
        }
        if (spanId != null) {
            MDC.put(SPAN_ID_KEY, spanId);
        }
    }

    public static String getTraceId() {
        return CURRENT_TRACE_ID.get();
    }

    public static String getSpanId() {
        return CURRENT_SPAN_ID.get();
    }

    public static void clear() {
        CURRENT_TRACE_ID.remove();
        CURRENT_SPAN_ID.remove();
        MDC.remove(TRACE_ID_KEY);
        MDC.remove(SPAN_ID_KEY);
    }

    public static String childSpanId() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
