package com.evaluation.prototype.hod.controller;

import java.time.YearMonth;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.evaluation.prototype.academic.dto.BatchResponse;
import com.evaluation.prototype.academic.repository.BatchRepository;
import com.evaluation.prototype.auth.service.CurrentUserService;
import com.evaluation.prototype.common.exception.ApiException;
import com.evaluation.prototype.common.imports.ExcelTableReader;
import com.evaluation.prototype.hod.dto.ExamRegistrationImportResponse;
import com.evaluation.prototype.hod.service.ExamRegistrationImportService;
import com.evaluation.prototype.student.dto.StudentImportResponse;
import com.evaluation.prototype.student.dto.StudentUpsertRequest;
import com.evaluation.prototype.student.service.StudentService;
import com.evaluation.prototype.user.entity.User;

import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/hod")
@PreAuthorize("hasRole('HOD')")
public class HodImportController {

    private final ExcelTableReader excelTableReader;
    private final BatchRepository batchRepository;
    private final StudentService studentService;
    private final ExamRegistrationImportService examRegistrationImportService;
    private final CurrentUserService currentUserService;

    public HodImportController(
            ExcelTableReader excelTableReader,
            BatchRepository batchRepository,
            StudentService studentService,
            ExamRegistrationImportService examRegistrationImportService,
            CurrentUserService currentUserService) {
        this.excelTableReader = excelTableReader;
        this.batchRepository = batchRepository;
        this.studentService = studentService;
        this.examRegistrationImportService = examRegistrationImportService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/batches")
    @PreAuthorize("hasAnyRole('HOD', 'ADMIN')")
    public List<BatchResponse> batches() {
        return batchRepository.findAllImportOptions();
    }

    @PostMapping("/students/import")
    public StudentImportResponse importStudents(
            @RequestParam("file") MultipartFile file,
            @RequestParam("batchId") @NotNull UUID batchId) {
        if (!batchRepository.existsById(batchId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The selected course batch does not exist.");
        }
        var rows = excelTableReader.read(file, Set.of("registernumber", "studentname"));
        List<StudentUpsertRequest> requests = new ArrayList<>(rows.size());
        Set<String> registerNumbers = new HashSet<>();
        for (var row : rows) {
            String registerNumber = normalize(row.get("registernumber"));
            String studentName = row.get("studentname").trim();
            if (registerNumber.isEmpty() || studentName.isEmpty()) {
                throw rowError(row.rowNumber(), "Register number and student name are required.");
            }
            if (!registerNumbers.add(registerNumber)) {
                throw rowError(row.rowNumber(), "Duplicate register number in this file.");
            }
            LocalDate dateOfBirth = parseDate(row.get("dateofbirth"));
            if (!row.get("dateofbirth").isBlank() && dateOfBirth == null) {
                throw rowError(row.rowNumber(), "Date of birth must be YYYY-MM-DD or DD-MM-YYYY.");
            }
            requests.add(new StudentUpsertRequest(
                    registerNumber,
                    studentName,
                    batchId,
                    dateOfBirth,
                    blankToNull(row.get("category")),
                    blankToNull(row.get("gender"))));
        }
        try {
            return studentService.importBatch(requests, actor());
        } catch (DataIntegrityViolationException exception) {
            throw importConflict();
        }
    }

    @PostMapping("/exam-registrations/import")
    public ExamRegistrationImportResponse importExamRegistrations(@RequestParam("file") MultipartFile file) {
        var rows = excelTableReader.read(file, Set.of("registernumber", "qpcode", "examyearmonth"));
        List<ExamRegistrationImportService.ExamRegistrationRow> registrations = new ArrayList<>(rows.size());
        Set<String> uniqueExamRows = new HashSet<>();

        for (var row : rows) {
            String registerNumber = normalize(row.get("registernumber"));
            String qpCode = normalize(row.get("qpcode"));
            String rawYearMonth = row.get("examyearmonth").trim();
            List<String> missingFields = new ArrayList<>();
            if (registerNumber.isEmpty()) {
                missingFields.add("USN/register number");
            }
            if (qpCode.isEmpty()) {
                missingFields.add("QP code");
            }
            if (rawYearMonth.isEmpty()) {
                missingFields.add("year-month");
            }
            if (!missingFields.isEmpty()) {
                throw rowError(row.rowNumber(), String.join(", ", missingFields) + " required.");
            }
            String yearMonth = parseYearMonth(rawYearMonth);
            if (yearMonth == null) {
                throw rowError(row.rowNumber(),
                        "Year-month '" + rawYearMonth + "' is invalid. Use YYYY-MM (for example, 2026-10).");
            }
            String examKey = qpCode + "|" + registerNumber + "|" + yearMonth;
            if (!uniqueExamRows.add(examKey)) {
                throw rowError(row.rowNumber(), "Duplicate QP code, USN, and year-month row.");
            }
            registrations.add(new ExamRegistrationImportService.ExamRegistrationRow(
                    row.rowNumber(), registerNumber, qpCode, yearMonth));
        }

        try {
            return examRegistrationImportService.importRows(registrations, actor());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (DataIntegrityViolationException exception) {
            throw importConflict();
        }
    }

    private ApiException importConflict() {
        return new ApiException(HttpStatus.CONFLICT,
                "The import conflicts with existing data or required references. Check duplicate rows, student USNs, QP codes, and the selected batch.");
    }

    private User actor() {
        return currentUserService.getCurrentUser()
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated."));
    }

    private ApiException rowError(int rowNumber, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "Excel row " + rowNumber + ": " + message);
    }

    private String parseYearMonth(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        try {
            return YearMonth.parse(normalized).toString();
        } catch (DateTimeParseException ignored) {
            try {
                return YearMonth.parse(normalized, DateTimeFormatter.ofPattern("M/uuuu")).toString();
            } catch (DateTimeParseException ignoredAgain) {
                LocalDate date = parseDate(normalized);
                return date == null ? null : YearMonth.from(date).toString();
            }
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        for (DateTimeFormatter formatter : List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("d-M-uuuu"),
                DateTimeFormatter.ofPattern("d/M/uuuu"))) {
            try {
                return LocalDate.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next supported date format.
            }
        }
        return null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
