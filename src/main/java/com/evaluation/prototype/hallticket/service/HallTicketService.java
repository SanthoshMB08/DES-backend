package com.evaluation.prototype.hallticket.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.evaluation.prototype.attendance.entity.Attendance;
import com.evaluation.prototype.attendance.service.AttendanceService;
import com.evaluation.prototype.audit.service.AuditService;
import com.evaluation.prototype.hallticket.dto.HallTicketExamRequest;
import com.evaluation.prototype.hallticket.dto.HallTicketScanRequest;
import com.evaluation.prototype.hallticket.entity.HallTicket;
import com.evaluation.prototype.hallticket.entity.HallTicketExam;
import com.evaluation.prototype.hallticket.repository.HallTicketExamRepository;
import com.evaluation.prototype.hallticket.repository.HallTicketRepository;
import com.evaluation.prototype.student.entity.Student;
import com.evaluation.prototype.student.service.StudentService;
import com.evaluation.prototype.user.entity.User;

@Service
public class HallTicketService {

    private final StudentService studentService;
    private final HallTicketRepository hallTicketRepository;
    private final HallTicketExamRepository hallTicketExamRepository;
    private final AttendanceService attendanceService;
    private final AuditService auditService;

    public HallTicketService(
            StudentService studentService,
            HallTicketRepository hallTicketRepository,
            HallTicketExamRepository hallTicketExamRepository,
            AttendanceService attendanceService,
            AuditService auditService) {
        this.studentService = studentService;
        this.hallTicketRepository = hallTicketRepository;
        this.hallTicketExamRepository = hallTicketExamRepository;
        this.attendanceService = attendanceService;
        this.auditService = auditService;
    }

    @Transactional
    public List<Attendance> scan(HallTicketScanRequest request, User actor) {
        Student student = studentService.findOrCreateAndLinkAttendance(request.registerNumber(), request.studentName());
        String registerNumber = studentService.normalize(request.registerNumber());
        String qrHash = buildQrHash(registerNumber, request);

        HallTicket hallTicket = hallTicketRepository.findByQrHash(qrHash)
                .orElseGet(() -> createHallTicket(student, registerNumber, request, qrHash, actor));

        List<Attendance> attendances = request.exams().stream()
                .map(exam -> createHallTicketExamIfMissing(hallTicket, exam))
                .map(exam -> attendanceService.ensurePendingAttendance(
                        student,
                        registerNumber,
                        exam.getExamDate(),
                        exam.getQpCode()))
                .toList();

        auditService.record(actor, "HallTicket", hallTicket.getId(), "SCAN", null,
                "{\"registerNumber\":\"%s\",\"examCount\":%d}".formatted(registerNumber, request.exams().size()));

        return attendances;
    }

    private HallTicket createHallTicket(
            Student student,
            String registerNumber,
            HallTicketScanRequest request,
            String qrHash,
            User actor) {
        HallTicket hallTicket = new HallTicket();
        hallTicket.setStudent(student);
        hallTicket.setRegisterNumber(registerNumber);
        hallTicket.setStudentType(request.studentType());
        hallTicket.setQrHash(qrHash);
        hallTicket.setScannedBy(actor);
        return hallTicketRepository.save(hallTicket);
    }

    private HallTicketExam createHallTicketExamIfMissing(HallTicket hallTicket, HallTicketExamRequest request) {
        String qpCode = studentService.normalize(request.qpCode());
        return hallTicketExamRepository
                .findByHallTicket_IdAndExamDateAndQpCode(hallTicket.getId(), request.examDate(), qpCode)
                .orElseGet(() -> {
                    HallTicketExam exam = new HallTicketExam();
                    exam.setHallTicket(hallTicket);
                    exam.setExamDate(request.examDate());
                    exam.setQpCode(qpCode);
                    return hallTicketExamRepository.save(exam);
                });
    }

    private String buildQrHash(String registerNumber, HallTicketScanRequest request) {
        String exams = request.exams().stream()
                .sorted(Comparator.comparing(HallTicketExamRequest::examDate).thenComparing(HallTicketExamRequest::qpCode))
                .map(exam -> exam.examDate() + ":" + studentService.normalize(exam.qpCode()))
                .reduce("", (left, right) -> left + "|" + right);
        String raw = registerNumber + "|" + request.studentType() + exams;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
