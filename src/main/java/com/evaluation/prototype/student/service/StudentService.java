package com.evaluation.prototype.student.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.evaluation.prototype.academic.repository.BatchRepository;
import com.evaluation.prototype.audit.service.AuditService;
import com.evaluation.prototype.common.exception.ApiException;
import com.evaluation.prototype.student.dto.StudentImportResponse;
import com.evaluation.prototype.student.dto.StudentResponse;
import com.evaluation.prototype.student.dto.StudentUpsertRequest;
import com.evaluation.prototype.student.entity.Student;
import com.evaluation.prototype.student.repository.StudentRepository;
import com.evaluation.prototype.user.entity.User;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final BatchRepository batchRepository;
    private final AuditService auditService;

    public StudentService(
            StudentRepository studentRepository,
            BatchRepository batchRepository,
            AuditService auditService) {
        this.studentRepository = studentRepository;
        this.batchRepository = batchRepository;
        this.auditService = auditService;
    }

    /**
     * HOD-facing: upsert a student's profile before exam registrations are imported.
     */
    @Transactional
    public StudentResponse upsert(StudentUpsertRequest request) {
        String registerNumber = normalize(request.registerNumber());
        Student student = studentRepository.findByRegisterNumber(registerNumber).orElseGet(() -> {
            Student s = new Student();
            s.setRegisterNumber(registerNumber);
            return s;
        });
        if (StringUtils.hasText(request.studentName())) {
            student.setStudentName(request.studentName().trim());
        }
        if (request.batchId() != null) {
            student.setBatch(batchRepository.findById(request.batchId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Batch not found")));
        }
        if (request.dateOfBirth() != null) {
            student.setDateOfBirth(request.dateOfBirth());
        }
        if (StringUtils.hasText(request.category())) {
            student.setCategory(request.category().trim());
        }
        if (StringUtils.hasText(request.gender())) {
            student.setGender(request.gender().trim());
        }
        return StudentResponse.from(studentRepository.save(student));
    }

    @Transactional
    public StudentImportResponse importBatch(List<StudentUpsertRequest> requests, User actor) {
        java.util.Set<String> registerNumbers = new java.util.HashSet<>();
        for (StudentUpsertRequest request : requests) {
            String normalizedRegisterNumber = normalize(request.registerNumber());
            if (!StringUtils.hasText(normalizedRegisterNumber) || !registerNumbers.add(normalizedRegisterNumber)) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "The spreadsheet contains a missing or duplicate register number.");
            }
            if (request.batchId() == null || !batchRepository.existsById(request.batchId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Select a valid course batch before importing students.");
            }
        }

        int created = 0;
        int updated = 0;
        for (StudentUpsertRequest request : requests) {
            if (studentRepository.findByRegisterNumber(normalize(request.registerNumber())).isPresent()) {
                updated++;
            } else {
                created++;
            }
            upsert(request);
        }
        auditService.record(actor, "Student", null, "BULK_IMPORT", null,
                "{\"rowsProcessed\":%d,\"studentsCreated\":%d,\"studentsUpdated\":%d}"
                        .formatted(requests.size(), created, updated));
        return new StudentImportResponse(requests.size(), created, updated);
    }

    public List<StudentResponse> search(String registerNumber) {
        return studentRepository.findByRegisterNumberContainingIgnoreCase(normalize(registerNumber)).stream()
                .map(StudentResponse::from)
                .toList();
    }

    public List<StudentResponse> listForBatch(UUID batchId) {
        return studentRepository.findByBatchIdOrderByRegisterNumberAsc(batchId).stream()
                .map(StudentResponse::from)
                .toList();
    }

    public String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

}
