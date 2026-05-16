package com.workhub.saasbackend.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.workhub.saasbackend.entity.AuditActionResult;
import com.workhub.saasbackend.entity.AuditEventType;
import com.workhub.saasbackend.entity.Job;
import com.workhub.saasbackend.entity.JobStatus;
import com.workhub.saasbackend.observability.BusinessMetrics;
import com.workhub.saasbackend.repository.JobRepository;
import com.workhub.saasbackend.service.AuditService;

@ExtendWith(MockitoExtension.class)
class JobConsumerTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private BusinessMetrics businessMetrics;

    private JobConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new JobConsumer(jobRepository, auditService, businessMetrics);
    }

    @Test
    void handleJobMessage_pendingJob_shouldWriteProcessingAndCompletedAudit() {
        UUID jobId = UUID.randomUUID();
        Job job = pendingJob(jobId, "tenant-a");
        JobMessage message = new JobMessage(jobId, "tenant-a");

        when(jobRepository.findByIdAndTenantId(jobId, "tenant-a")).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        consumer.handleJobMessage(message);

        ArgumentCaptor<AuditEventType> types = ArgumentCaptor.forClass(AuditEventType.class);
        verify(auditService, times(2)).record(
                eq("tenant-a"),
                types.capture(),
                eq(null),
                eq("job"),
                eq(jobId.toString()),
                eq(AuditActionResult.SUCCESS),
                eq(null));
        assertThat(types.getAllValues())
                .contains(AuditEventType.JOB_PROCESSING, AuditEventType.JOB_COMPLETED);
        verify(businessMetrics).recordJobCompleted("tenant-a", true);
    }

    @Test
    void handleJobMessage_terminalDoneJob_shouldNotReprocessOrAudit() {
        UUID jobId = UUID.randomUUID();
        Job job = pendingJob(jobId, "tenant-a");
        job.setStatus(JobStatus.DONE);
        JobMessage message = new JobMessage(jobId, "tenant-a");

        when(jobRepository.findByIdAndTenantId(jobId, "tenant-a")).thenReturn(Optional.of(job));

        consumer.handleJobMessage(message);

        verify(jobRepository, never()).save(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void handleJobMessage_saveFailure_shouldMarkFailedAndAudit() {
        UUID jobId = UUID.randomUUID();
        Job job = pendingJob(jobId, "tenant-a");
        JobMessage message = new JobMessage(jobId, "tenant-a");

        when(jobRepository.findByIdAndTenantId(jobId, "tenant-a")).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class)))
                .thenAnswer(invocation -> invocation.getArgument(0))
                .thenThrow(new RuntimeException("db error"));

        consumer.handleJobMessage(message);

        verify(auditService).record(
                eq("tenant-a"),
                eq(AuditEventType.JOB_FAILED),
                eq(null),
                eq("job"),
                eq(jobId.toString()),
                eq(AuditActionResult.FAILURE),
                eq("db error"));
        verify(businessMetrics).recordJobCompleted("tenant-a", false);
    }

    private static Job pendingJob(UUID jobId, String tenantId) {
        Job job = new Job();
        job.setId(jobId);
        job.setTenantId(tenantId);
        job.setStatus(JobStatus.PENDING);
        return job;
    }
}
