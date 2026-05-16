package com.workhub.saasbackend.observability;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.workhub.saasbackend.security.TenantContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class RequestObservabilityFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        long startedAt = System.currentTimeMillis();
        String method = request.getMethod();
        String path = request.getRequestURI();
        String correlationId = request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            BusinessLogger.info(
                    "HTTP_REQUEST",
                    "request completed",
                    "method", method,
                    "path", path,
                    "status", response.getStatus(),
                    "durationMs", durationMs,
                    "correlationId", correlationId != null ? correlationId : "",
                    "tenantId", TenantContext.getTenantId() != null ? TenantContext.getTenantId() : ""
            );
        }
    }
}
