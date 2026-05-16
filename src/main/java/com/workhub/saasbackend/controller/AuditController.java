package com.workhub.saasbackend.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workhub.saasbackend.dto.response.AuditEventResponse;
import com.workhub.saasbackend.dto.response.PagedResponse;
import com.workhub.saasbackend.service.AuditService;

@RestController
@RequestMapping("/admin/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public PagedResponse<AuditEventResponse> listAuditEvents(
            @PageableDefault(size = 50, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return auditService.listForTenant(pageable);
    }
}
