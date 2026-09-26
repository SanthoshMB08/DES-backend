package com.evaluation.prototype.admin.dto;

import java.util.List;

public record AdminDashboardResponse(
        long totalUsers,
        long totalInvigilators,
        long totalStudents,
        long totalHallTicketsScanned,
        long totalPresentStudents,
        long totalAbsentStudents,
        long totalBookletsAssigned,
        long todaysActivity,
        List<AuditLogResponse> recentAuditEvents) {
}
