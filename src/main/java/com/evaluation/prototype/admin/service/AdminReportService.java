package com.evaluation.prototype.admin.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.evaluation.prototype.admin.dto.AdminDashboardResponse;
import com.evaluation.prototype.admin.dto.AuditLogResponse;
import com.evaluation.prototype.audit.entity.AuditLog;
import com.evaluation.prototype.audit.repository.AuditLogRepository;
import com.evaluation.prototype.booklet.repository.BookletRecordRepository;
import com.evaluation.prototype.common.enums.UserStatus;
import com.evaluation.prototype.hallticket.repository.HallTicketDataRepository;
import com.evaluation.prototype.student.repository.StudentRepository;
import com.evaluation.prototype.user.entity.User;
import com.evaluation.prototype.user.repository.UserRepository;

@Service
public class AdminReportService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final HallTicketDataRepository hallTicketDataRepository;
    private final BookletRecordRepository bookletRecordRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminReportService(
            UserRepository userRepository,
            StudentRepository studentRepository,
            HallTicketDataRepository hallTicketDataRepository,
            BookletRecordRepository bookletRecordRepository,
            AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.hallTicketDataRepository = hallTicketDataRepository;
        this.bookletRecordRepository = bookletRecordRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard() {
        LocalDate today = LocalDate.now();
        long todaysActivity = auditLogRepository.countByCreatedAtBetween(
                today.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        return new AdminDashboardResponse(
                userRepository.countByStatusAndDeletedAtIsNull(UserStatus.ACTIVE),
                studentRepository.count(),
                hallTicketDataRepository.count(),
                bookletRecordRepository.count(),
                todaysActivity,
                auditLogRepository.findTop10ByOrderByCreatedAtDesc().stream()
                        .map(AuditLogResponse::from)
                        .toList());
    }

    public List<User> userReport() {
        return userRepository.findByDeletedAtIsNull();
    }

    public List<AuditLog> failedLoginReport() {
        return auditLogRepository.findAll().stream()
                .filter(log -> "LOGIN_FAILED".equals(log.getAction()))
                .toList();
    }

    public List<AuditLog> duplicateScanReport() {
        return auditLogRepository.findAll().stream()
                .filter(log -> "SCAN".equals(log.getAction()))
                .toList();
    }
}
