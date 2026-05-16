package com.workhub.saasbackend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.workhub.saasbackend.entity.AuditEvent;
import com.workhub.saasbackend.entity.AuditEventType;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    Page<AuditEvent> findAllByTenantIdOrderByOccurredAtDesc(String tenantId, Pageable pageable);

    List<AuditEvent> findByTenantIdAndEventTypeOrderByOccurredAtDesc(String tenantId, AuditEventType eventType);

    long countByTenantId(String tenantId);
}
