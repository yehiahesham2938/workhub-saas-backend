package com.workhub.saasbackend.observability;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class BusinessMetrics {

    private final MeterRegistry registry;

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordJobCreated(String tenantId) {
        counter("workhub.jobs.created", "tenant", tenantId).increment();
    }

    public void recordJobCompleted(String tenantId, boolean success) {
        counter("workhub.jobs.completed", "tenant", tenantId, "result", success ? "success" : "failure").increment();
    }

    public void recordJobPublishFailed(String tenantId) {
        counter("workhub.jobs.publish.failed", "tenant", tenantId).increment();
    }

    public void recordJobRetry(String tenantId) {
        counter("workhub.jobs.retry", "tenant", tenantId).increment();
    }

    public void recordProjectCreated(String tenantId) {
        counter("workhub.projects.created", "tenant", tenantId).increment();
    }

    public Timer.Sample startProjectCreateTimer() {
        return Timer.start(registry);
    }

    public void recordProjectCreateDuration(Timer.Sample sample, String tenantId) {
        sample.stop(timer("workhub.projects.create.duration", "tenant", tenantId));
    }

    public void recordCrossTenantDenied(String tenantId, String resourceType) {
        counter("workhub.security.cross_tenant.denied", "tenant", tenantId, "resource", resourceType).increment();
    }

    public void recordQuotaExceeded(String tenantId, String resourceType) {
        counter("workhub.quotas.exceeded", "tenant", tenantId, "resource", resourceType).increment();
    }

    public void recordAuditEvent(String tenantId, String eventType) {
        counter("workhub.audit.events", "tenant", tenantId, "event", eventType).increment();
    }

    public void recordSagaCompleted(String tenantId, boolean success) {
        counter("workhub.saga.completed", "tenant", tenantId, "result", success ? "success" : "compensated").increment();
    }

    private Counter counter(String name, String... tags) {
        return Counter.builder(name).tags(tags).register(registry);
    }

    private Timer timer(String name, String... tags) {
        return Timer.builder(name).tags(tags).publishPercentiles(0.5, 0.95).register(registry);
    }
}
