package com.evaluation.prototype.hod.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.evaluation.prototype.academic.repository.AcademicSubjectRepository;
import com.evaluation.prototype.audit.service.AuditService;
import com.evaluation.prototype.hallticket.repository.HallTicketDataRepository;
import com.evaluation.prototype.hod.dto.ExamRegistrationImportResponse;
import com.evaluation.prototype.student.repository.StudentRepository;
import com.evaluation.prototype.user.entity.User;
import com.evaluation.prototype.valuation.repository.SubjectRepository;

@Service
public class ExamRegistrationImportService {

    private final StudentRepository studentRepository;
    private final AcademicSubjectRepository academicSubjectRepository;
    private final SubjectRepository subjectRepository;
    private final HallTicketDataRepository hallTicketDataRepository;
    private final AuditService auditService;

    public ExamRegistrationImportService(
            StudentRepository studentRepository,
            AcademicSubjectRepository academicSubjectRepository,
            SubjectRepository subjectRepository,
            HallTicketDataRepository hallTicketDataRepository,
            AuditService auditService) {
        this.studentRepository = studentRepository;
        this.academicSubjectRepository = academicSubjectRepository;
        this.subjectRepository = subjectRepository;
        this.hallTicketDataRepository = hallTicketDataRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ExamRegistrationImportResponse importRows(List<ExamRegistrationRow> rows, User actor) {
        int created = 0;
        int alreadyPresent = 0;

        for (ExamRegistrationRow row : rows) {
            String registerNumber = normalize(row.registerNumber());
            String qpCode = normalize(row.qpCode());
            var student = studentRepository.findByRegisterNumber(registerNumber)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Excel row " + row.rowNumber() + ": student " + registerNumber
                                    + " is not in the student list. Import the student data first."));
            if (!academicSubjectRepository.existsByQpCodeIgnoreCase(qpCode)
                    && !subjectRepository.existsByQpCodeIgnoreCase(qpCode)) {
                throw new IllegalArgumentException(
                        "Excel row " + row.rowNumber() + ": QP code " + qpCode + " is not registered as a subject.");
            }
            if (hallTicketDataRepository.findByQpCodeIgnoreCaseAndUsnIgnoreCaseAndExamYearMonth(
                    qpCode, registerNumber, row.examYearMonth()).isPresent()) {
                alreadyPresent++;
                continue;
            }
            int inserted = hallTicketDataRepository.insertIfAbsent(
                    UUID.randomUUID(), student.getId(), registerNumber, qpCode, row.examYearMonth(), actor.getId());
            if (inserted == 1) {
                created++;
            } else {
                alreadyPresent++;
            }
        }

        auditService.record(actor, "HallTicketData", null, "BULK_IMPORT", null,
                "{\"rowsProcessed\":%d,\"registrationsCreated\":%d,\"registrationsAlreadyPresent\":%d}"
                        .formatted(rows.size(), created, alreadyPresent));
        return new ExamRegistrationImportResponse(rows.size(), created, alreadyPresent);
    }

    private String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    public record ExamRegistrationRow(int rowNumber, String registerNumber, String qpCode, String examYearMonth) {
    }
}
