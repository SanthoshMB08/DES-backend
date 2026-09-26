package com.evaluation.prototype.admin.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.evaluation.prototype.admin.dto.AuditLogResponse;
import com.evaluation.prototype.admin.service.AdminAuditService;

@RestController
@RequestMapping("/api/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    public AdminAuditController(AdminAuditService adminAuditService) {
        this.adminAuditService = adminAuditService;
    }

    @GetMapping
    public Page<AuditLogResponse> search(
            @RequestParam(required = false) String user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String registerNumber,
            @RequestParam(required = false) String qpCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return adminAuditService.search(user, fromDate, toDate, action, registerNumber, qpCode, page, size);
    }

    @GetMapping("/{id}")
    public AuditLogResponse get(@PathVariable UUID id) {
        return adminAuditService.get(id);
    }
}
