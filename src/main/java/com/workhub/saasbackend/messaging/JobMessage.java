package com.workhub.saasbackend.messaging;

import java.io.Serializable;
import java.util.UUID;

public class JobMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID jobId;
    private String tenantId;
    private String traceId;
    private String spanId;

    public JobMessage() {
    }

    public JobMessage(UUID jobId, String tenantId) {
        this.jobId = jobId;
        this.tenantId = tenantId;
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }

    @Override
    public String toString() {
        return "JobMessage{jobId=" + jobId + ", tenantId='" + tenantId
                + "', traceId='" + traceId + "', spanId='" + spanId + "'}";
    }
}
