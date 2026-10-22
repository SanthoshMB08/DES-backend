package com.evaluation.prototype.admin.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.evaluation.prototype.admin.dto.AdminDashboardResponse;
import com.evaluation.prototype.admin.dto.AuditLogResponse;
import com.evaluation.prototype.admin.dto.UserResponse;
import com.evaluation.prototype.admin.service.AdminReportService;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final AdminReportService adminReportService;

    public AdminReportController(AdminReportService adminReportService) {
        this.adminReportService = adminReportService;
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        return adminReportService.dashboard();
    }

    @GetMapping("/reports/users")
    public List<UserResponse> userReport() {
        return adminReportService.userReport().stream().map(UserResponse::from).toList();
    }

    @GetMapping("/reports/failed-logins")
    public List<AuditLogResponse> failedLoginReport() {
        return adminReportService.failedLoginReport().stream().map(AuditLogResponse::from).toList();
    }

    @GetMapping("/reports/duplicate-scans")
    public List<AuditLogResponse> duplicateScanReport() {
        return adminReportService.duplicateScanReport().stream().map(AuditLogResponse::from).toList();
    }
}
