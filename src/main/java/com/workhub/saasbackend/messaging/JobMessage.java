package com.workhub.saasbackend.messaging;

import java.io.Serializable;
import java.util.UUID;

public class JobMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID jobId;
    private String tenantId;

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

    @Override
    public String toString() {
        return "JobMessage{jobId=" + jobId + ", tenantId='" + tenantId + "'}";
    }
}
