package com.evaluation.prototype.hallticket.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.evaluation.prototype.audit.service.AuditService;
import com.evaluation.prototype.common.enums.StudentType;
import com.evaluation.prototype.hallticket.dto.CourseImportRecord;
import com.evaluation.prototype.hallticket.dto.HallTicketImportRequest;
import com.evaluation.prototype.hallticket.dto.HallTicketImportResponse;
import com.evaluation.prototype.hallticket.dto.StudentImportRecord;
import com.evaluation.prototype.hallticket.entity.HallTicket;
import com.evaluation.prototype.hallticket.entity.HallTicketExam;
import com.evaluation.prototype.hallticket.repository.HallTicketExamRepository;
import com.evaluation.prototype.hallticket.repository.HallTicketRepository;
import com.evaluation.prototype.student.entity.Student;
import com.evaluation.prototype.student.repository.StudentRepository;
import com.evaluation.prototype.user.entity.User;

/**
 * Bulk-loads hall ticket data extracted by the OCR module (see the
 * ocr-module repo's apps/worker/extract.py) as pre-data - Student and
 * HallTicket/HallTicketExam rows that exist before any invigilator has
 * scanned anything. This is the "base data" the barcode-first invigilator
 * flow (MobileInvigilatorService.lookupByBarcode) relies on.
 * <p>
 * Idempotent: importing the same file twice (or an updated re-export)
 * updates existing rows rather than duplicating them, matched on register
 * number + semester for the hall ticket, and exam date + QP code for each
 * exam row - the same keys already used everywhere else in this app.
 */
@Service
public class HallTicketImportService {

    // The OCR module normalizes exam_date to this format; falling back to the
    // original DD-MM-YYYY format from the source PDF in case a row didn't parse
    // cleanly on the OCR side.
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final StudentRepository studentRepository;
    private final HallTicketRepository hallTicketRepository;
    private final HallTicketExamRepository hallTicketExamRepository;
    private final AuditService auditService;

    public HallTicketImportService(
            StudentRepository studentRepository,
            HallTicketRepository hallTicketRepository,
            HallTicketExamRepository hallTicketExamRepository,
            AuditService auditService) {
        this.studentRepository = studentRepository;
        this.hallTicketRepository = hallTicketRepository;
        this.hallTicketExamRepository = hallTicketExamRepository;
        this.auditService = auditService;
    }

    @Transactional
    public HallTicketImportResponse importHallTickets(HallTicketImportRequest request, User actor) {
        int hallTicketsCreated = 0;
        int examRowsCreated = 0;
        int examRowsUpdated = 0;
        List<String> warnings = new ArrayList<>();

        for (StudentImportRecord record : request.students()) {
            String registerNumber = normalize(record.uucms());
            String semester = normalize(record.semester());
            if (!StringUtils.hasText(registerNumber) || !StringUtils.hasText(semester)) {
                warnings.add("Skipped a record with a missing register number or semester");
                continue;
            }

            Student student = studentRepository.findByRegisterNumber(registerNumber).orElseGet(() -> {
                Student s = new Student();
                s.setRegisterNumber(registerNumber);
                return s;
            });
            if (StringUtils.hasText(record.studentName()) && !record.studentName().trim().equals(student.getStudentName())) {
                student.setStudentName(record.studentName().trim());
            }
            student = studentRepository.save(student);

            var existingTicket = hallTicketRepository.findByRegisterNumberAndSemesterIgnoreCase(registerNumber, semester);
            HallTicket hallTicket;
            if (existingTicket.isPresent()) {
                hallTicket = existingTicket.get();
            } else {
                hallTicket = new HallTicket();
                hallTicket.setStudent(student);
                hallTicket.setRegisterNumber(registerNumber);
                hallTicket.setSemester(semester);
                // The OCR-extracted hall ticket doesn't carry fresher/repeater status -
                // default to FRESHER, the common case for a bulk semester-wide import.
                hallTicket.setStudentType(StudentType.FRESHER);
                hallTicket.setScannedBy(actor);
                hallTicket = hallTicketRepository.save(hallTicket);
                hallTicketsCreated++;
            }

            for (CourseImportRecord course : record.courses()) {
                if (!StringUtils.hasText(course.qpCode())) {
                    warnings.add(registerNumber + ": skipped a course row with no QP code");
                    continue;
                }
                LocalDate examDate = parseDate(course.examDate());
                if (examDate == null) {
                    warnings.add(registerNumber + " / " + course.qpCode() + ": could not parse exam date \"" + course.examDate() + "\" - row skipped");
                    continue;
                }
                String qpCode = normalize(course.qpCode());

                var existingExam = hallTicketExamRepository.findByHallTicket_IdAndExamDateAndQpCode(hallTicket.getId(), examDate, qpCode);
                HallTicketExam exam = existingExam.orElseGet(HallTicketExam::new);
                boolean isNew = existingExam.isEmpty();
                exam.setHallTicket(hallTicket);
                exam.setExamDate(examDate);
                exam.setQpCode(qpCode);
                exam.setCourseName(course.courseName());
                exam.setExamTime(course.examTime());
                exam.setCentreCode(course.centreCode());
                hallTicketExamRepository.save(exam);
                if (isNew) {
                    examRowsCreated++;
                } else {
                    examRowsUpdated++;
                }
            }
        }

        auditService.record(actor, "HallTicket", null, "BULK_IMPORT", null,
                "{\"studentsProcessed\":%d,\"hallTicketsCreated\":%d,\"examRowsCreated\":%d,\"examRowsUpdated\":%d}"
                        .formatted(request.students().size(), hallTicketsCreated, examRowsCreated, examRowsUpdated));

        return new HallTicketImportResponse(request.students().size(), hallTicketsCreated, examRowsCreated, examRowsUpdated, warnings);
    }

    private LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), ISO);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(value.trim(), DD_MM_YYYY);
            } catch (DateTimeParseException stillInvalid) {
                return null;
            }
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
