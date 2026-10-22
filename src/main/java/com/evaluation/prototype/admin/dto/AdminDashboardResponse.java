package com.evaluation.prototype.admin.dto;

import java.util.List;

public record AdminDashboardResponse(
        long totalUsers,
        long totalStudents,
        long totalExamRegistrations,
        long totalBooklets,
        long todaysActivity,
        List<AuditLogResponse> recentAuditEvents) {
}
