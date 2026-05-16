package com.workhub.saasbackend.observability;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Establishes trace/span IDs for each HTTP request and echoes them on the response.
 * Supports W3C {@code traceparent} or {@code X-Trace-Id} / {@code X-Span-Id} headers.
 */
@Component
@Order(1)
public class DistributedTraceFilter extends OncePerRequestFilter {

    private static final Pattern TRACEPARENT = Pattern.compile("00-([0-9a-f]{32})-([0-9a-f]{16})-[0-9a-f]{2}");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String traceId = null;
        String spanId = null;

        String traceparent = request.getHeader(TraceContext.TRACEPARENT_HEADER);
        if (traceparent != null) {
            Matcher matcher = TRACEPARENT.matcher(traceparent.trim());
            if (matcher.matches()) {
                traceId = matcher.group(1);
                spanId = matcher.group(2);
            }
        }

        if (traceId == null) {
            traceId = request.getHeader(TraceContext.TRACE_ID_HEADER);
        }
        if (spanId == null) {
            spanId = request.getHeader(TraceContext.SPAN_ID_HEADER);
        }

        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        if (spanId == null || spanId.isBlank()) {
            spanId = TraceContext.childSpanId();
        }

        TraceContext.set(traceId, spanId);
        response.setHeader(TraceContext.TRACE_ID_HEADER, traceId);
        response.setHeader(TraceContext.SPAN_ID_HEADER, spanId);
        response.setHeader(TraceContext.TRACEPARENT_HEADER, formatTraceparent(traceId, spanId));

        try {
            filterChain.doFilter(request, response);
        } finally {
            TraceContext.clear();
        }
    }

    static String formatTraceparent(String traceId, String spanId) {
        String normalizedTrace = traceId.replace("-", "");
        if (normalizedTrace.length() > 32) {
            normalizedTrace = normalizedTrace.substring(0, 32);
        } else if (normalizedTrace.length() < 32) {
            normalizedTrace = String.format("%32s", normalizedTrace).replace(' ', '0');
        }
        String normalizedSpan = spanId.replace("-", "");
        if (normalizedSpan.length() > 16) {
            normalizedSpan = normalizedSpan.substring(0, 16);
        } else if (normalizedSpan.length() < 16) {
            normalizedSpan = String.format("%16s", normalizedSpan).replace(' ', '0');
        }
        return "00-" + normalizedTrace + "-" + normalizedSpan + "-01";
    }
}
