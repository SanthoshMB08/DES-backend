package com.evaluation.prototype.attendance.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.evaluation.prototype.attendance.dto.BookletScanRequest;
import com.evaluation.prototype.attendance.dto.MarkAbsentRequest;
import com.evaluation.prototype.attendance.entity.Attendance;
import com.evaluation.prototype.attendance.repository.AttendanceRepository;
import com.evaluation.prototype.audit.service.AuditService;
import com.evaluation.prototype.booklet.entity.AnswerBooklet;
import com.evaluation.prototype.booklet.repository.AnswerBookletRepository;
import com.evaluation.prototype.common.enums.AnswerBookletStatus;
import com.evaluation.prototype.common.enums.AttendanceSource;
import com.evaluation.prototype.common.enums.AttendanceStatus;
import com.evaluation.prototype.common.exception.ApiException;
import com.evaluation.prototype.student.entity.Student;
import com.evaluation.prototype.user.entity.User;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final AnswerBookletRepository answerBookletRepository;
    private final AuditService auditService;

    public AttendanceService(
            AttendanceRepository attendanceRepository,
            AnswerBookletRepository answerBookletRepository,
            AuditService auditService) {
        this.attendanceRepository = attendanceRepository;
        this.answerBookletRepository = answerBookletRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Attendance ensurePendingAttendance(Student student, String registerNumber, LocalDate examDate, String qpCode) {
        String normalizedRegisterNumber = normalize(registerNumber);
        String normalizedQpCode = normalize(qpCode);
        return attendanceRepository
                .findByRegisterNumberAndExamDateAndQpCode(normalizedRegisterNumber, examDate, normalizedQpCode)
                .map(existing -> linkStudentIfMissing(existing, student))
                .orElseGet(() -> createPending(student, normalizedRegisterNumber, examDate, normalizedQpCode));
    }

    @Transactional
    public Attendance markPresent(BookletScanRequest request, User actor) {
        String registerNumber = normalize(request.registerNumber());
        String qpCode = normalize(request.qpCode());
        String bookletNumber = normalize(request.answerBookletNumber());

        Attendance attendance = attendanceRepository
                .findByRegisterNumberAndExamDateAndQpCode(registerNumber, request.examDate(), qpCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Attendance record not found. Scan hall ticket first or mark absent manually."));

        validateBookletAvailable(bookletNumber, attendance);
        String oldValues = attendanceSnapshot(attendance);

        attendance.setAnswerBookletNumber(bookletNumber);
        attendance.setStatus(AttendanceStatus.PRESENT);
        attendance.setSource(AttendanceSource.BOOKLET_SCAN);
        attendance.setMarkedBy(actor);
        attendance.setMarkedAt(Instant.now());
        Attendance saved = attendanceRepository.save(attendance);

        AnswerBooklet booklet = answerBookletRepository.findByBookletNumber(bookletNumber)
                .orElseGet(() -> createBooklet(bookletNumber));
        booklet.assignTo(saved, actor);
        answerBookletRepository.save(booklet);

        auditService.record(actor, "Attendance", saved.getId(), "MARK_PRESENT", oldValues, attendanceSnapshot(saved));
        return saved;
    }

    @Transactional
    public Attendance markAbsent(MarkAbsentRequest request, User actor) {
        String registerNumber = normalize(request.registerNumber());
        String qpCode = normalize(request.qpCode());
        String bookletNumber = StringUtils.hasText(request.answerBookletNumber())
                ? normalize(request.answerBookletNumber())
                : null;

        Attendance attendance = attendanceRepository
                .findByRegisterNumberAndExamDateAndQpCode(registerNumber, request.examDate(), qpCode)
                .orElseGet(() -> createAbsentShell(registerNumber, request.examDate(), qpCode));

        validateBookletAvailable(bookletNumber, attendance);
        String oldValues = attendanceSnapshot(attendance);

        attendance.setStatus(AttendanceStatus.ABSENT);
        attendance.setSource(AttendanceSource.MANUAL_ABSENT);
        attendance.setMarkedBy(actor);
        attendance.setMarkedAt(Instant.now());
        attendance.setAnswerBookletNumber(bookletNumber);
        Attendance saved = attendanceRepository.save(attendance);

        if (bookletNumber != null) {
            AnswerBooklet booklet = answerBookletRepository.findByBookletNumber(bookletNumber)
                    .orElseGet(() -> createBooklet(bookletNumber));
            booklet.assignTo(saved, actor);
            answerBookletRepository.save(booklet);
        }

        auditService.record(actor, "Attendance", saved.getId(), "MARK_ABSENT", oldValues, attendanceSnapshot(saved));
        return saved;
    }

    /**
     * Combines every supplied filter with AND.
     *
     * This used to be a chain of early-return branches (register number, else
     * date+qp, else status, else everything), which silently discarded filters:
     * an examDate on its own matched no branch and fell through to findAll(),
     * so the caller got every attendance row with a 200 instead of an error.
     */
    public List<Attendance> search(String registerNumber, LocalDate examDate, String qpCode, AttendanceStatus status) {
        Specification<Attendance> spec = Specification.where(null);

        if (StringUtils.hasText(registerNumber)) {
            String value = normalize(registerNumber);
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("registerNumber")), value));
        }
        if (examDate != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("examDate"), examDate));
        }
        if (StringUtils.hasText(qpCode)) {
            String value = normalize(qpCode);
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("qpCode")), value));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        // spec stays null when no filter was supplied, and Spring Data treats a
        // null specification as "no predicate", which returns everything.
        return attendanceRepository.findAll(spec);
    }

    private Attendance linkStudentIfMissing(Attendance attendance, Student student) {
        if (attendance.getStudent() == null && student != null) {
            attendance.setStudent(student);
            return attendanceRepository.save(attendance);
        }
        return attendance;
    }

    private Attendance createPending(Student student, String registerNumber, LocalDate examDate, String qpCode) {
        Attendance attendance = new Attendance();
        attendance.setStudent(student);
        attendance.setRegisterNumber(registerNumber);
        attendance.setExamDate(examDate);
        attendance.setQpCode(qpCode);
        attendance.setStatus(AttendanceStatus.PENDING);
        attendance.setSource(AttendanceSource.HALL_TICKET_SCAN);
        return attendanceRepository.save(attendance);
    }

    private Attendance createAbsentShell(String registerNumber, LocalDate examDate, String qpCode) {
        Attendance attendance = new Attendance();
        attendance.setRegisterNumber(registerNumber);
        attendance.setExamDate(examDate);
        attendance.setQpCode(qpCode);
        attendance.setStatus(AttendanceStatus.ABSENT);
        attendance.setSource(AttendanceSource.MANUAL_ABSENT);
        return attendance;
    }

    private AnswerBooklet createBooklet(String bookletNumber) {
        AnswerBooklet booklet = new AnswerBooklet();
        booklet.setBookletNumber(bookletNumber);
        booklet.setStatus(AnswerBookletStatus.AVAILABLE);
        return booklet;
    }

    private void validateBookletAvailable(String bookletNumber, Attendance currentAttendance) {
        if (!StringUtils.hasText(bookletNumber)) {
            return;
        }
        if (currentAttendance.getId() == null && attendanceRepository.existsByAnswerBookletNumber(bookletNumber)) {
            throw new ApiException(HttpStatus.CONFLICT, "Answer booklet number is already assigned.");
        }
        if (currentAttendance.getId() != null
                && attendanceRepository.existsByAnswerBookletNumberAndIdNot(bookletNumber, currentAttendance.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "Answer booklet number is already assigned.");
        }
    }

    private String attendanceSnapshot(Attendance attendance) {
        return """
                {"registerNumber":"%s","examDate":"%s","qpCode":"%s","status":"%s","answerBookletNumber":%s}
                """.formatted(
                attendance.getRegisterNumber(),
                attendance.getExamDate(),
                attendance.getQpCode(),
                attendance.getStatus(),
                attendance.getAnswerBookletNumber() == null ? "null" : "\"" + attendance.getAnswerBookletNumber() + "\"")
                .trim();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
