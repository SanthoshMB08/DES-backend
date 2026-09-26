package com.evaluation.prototype.admin.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.evaluation.prototype.admin.dto.AdminDashboardResponse;
import com.evaluation.prototype.admin.dto.AuditLogResponse;
import com.evaluation.prototype.attendance.entity.Attendance;
import com.evaluation.prototype.attendance.repository.AttendanceRepository;
import com.evaluation.prototype.audit.entity.AuditLog;
import com.evaluation.prototype.audit.repository.AuditLogRepository;
import com.evaluation.prototype.booklet.repository.AnswerBookletRepository;
import com.evaluation.prototype.common.enums.AnswerBookletStatus;
import com.evaluation.prototype.common.enums.AttendanceStatus;
import com.evaluation.prototype.common.enums.UserStatus;
import com.evaluation.prototype.hallticket.repository.HallTicketRepository;
import com.evaluation.prototype.student.repository.StudentRepository;
import com.evaluation.prototype.user.entity.User;
import com.evaluation.prototype.user.repository.UserRepository;

@Service
public class AdminReportService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final HallTicketRepository hallTicketRepository;
    private final AttendanceRepository attendanceRepository;
    private final AnswerBookletRepository answerBookletRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminReportService(
            UserRepository userRepository,
            StudentRepository studentRepository,
            HallTicketRepository hallTicketRepository,
            AttendanceRepository attendanceRepository,
            AnswerBookletRepository answerBookletRepository,
            AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.hallTicketRepository = hallTicketRepository;
        this.attendanceRepository = attendanceRepository;
        this.answerBookletRepository = answerBookletRepository;
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
                userRepository.countByRoles_NameAndDeletedAtIsNull("ROLE_INVIGILATOR"),
                studentRepository.count(),
                hallTicketRepository.count(),
                attendanceRepository.countByStatus(AttendanceStatus.PRESENT),
                attendanceRepository.countByStatus(AttendanceStatus.ABSENT),
                answerBookletRepository.countByStatus(AnswerBookletStatus.ASSIGNED),
                todaysActivity,
                auditLogRepository.findTop10ByOrderByCreatedAtDesc().stream()
                        .map(AuditLogResponse::from)
                        .toList());
    }

    public Map<String, Object> attendanceSummary() {
        return Map.of(
                "pending", attendanceRepository.countByStatus(AttendanceStatus.PENDING),
                "present", attendanceRepository.countByStatus(AttendanceStatus.PRESENT),
                "absent", attendanceRepository.countByStatus(AttendanceStatus.ABSENT),
                "total", attendanceRepository.count());
    }

    public List<Attendance> absentReport() {
        return attendanceRepository.findByStatus(AttendanceStatus.ABSENT);
    }

    public List<Attendance> bookletReport() {
        return attendanceRepository.findAll().stream()
                .filter(attendance -> attendance.getAnswerBookletNumber() != null)
                .toList();
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
