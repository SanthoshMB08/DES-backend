package com.evaluation.prototype.valuation.service;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.evaluation.prototype.auth.service.CurrentUserService;
import com.evaluation.prototype.booklet.entity.BookletRecord;
import com.evaluation.prototype.booklet.repository.BookletRecordRepository;
import com.evaluation.prototype.common.exception.ApiException;
import com.evaluation.prototype.hallticket.entity.HallTicketData;
import com.evaluation.prototype.hallticket.repository.HallTicketDataRepository;
import com.evaluation.prototype.storage.SupabaseStorageService;
import com.evaluation.prototype.user.entity.User;
import com.evaluation.prototype.valuation.dto.ScannerBookletSubmitResponse;
import com.evaluation.prototype.valuation.dto.ScannerRegistrationResponse;
import com.evaluation.prototype.valuation.entity.Subject;
import com.evaluation.prototype.valuation.entity.ValuationScript;
import com.evaluation.prototype.valuation.enums.ValuationStatus;
import com.evaluation.prototype.valuation.repository.SubjectRepository;
import com.evaluation.prototype.valuation.repository.ValuationScriptRepository;

@Service
public class ScannerBookletService {

    private final HallTicketDataRepository hallTicketDataRepository;
    private final BookletRecordRepository bookletRecordRepository;
    private final ValuationScriptRepository scriptRepository;
    private final SubjectRepository subjectRepository;
    private final CurrentUserService currentUserService;
    private final SupabaseStorageService storageService;

    public ScannerBookletService(
            HallTicketDataRepository hallTicketDataRepository,
            BookletRecordRepository bookletRecordRepository,
            ValuationScriptRepository scriptRepository,
            SubjectRepository subjectRepository,
            CurrentUserService currentUserService,
            SupabaseStorageService storageService) {
        this.hallTicketDataRepository = hallTicketDataRepository;
        this.bookletRecordRepository = bookletRecordRepository;
        this.scriptRepository = scriptRepository;
        this.subjectRepository = subjectRepository;
        this.currentUserService = currentUserService;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public List<ScannerRegistrationResponse> findRegistrations(String usn, String qpCode, String examYearMonth) {
        String normalizedUsn = requireText(usn, "USN is required").toUpperCase();
        String normalizedQpCode = requireText(qpCode, "QP code is required").toUpperCase();
        String normalizedExamYearMonth = examYearMonth == null ? "" : examYearMonth.trim();
        if (!normalizedExamYearMonth.isEmpty()) {
            validateExamYearMonth(normalizedExamYearMonth);
        }

        return hallTicketDataRepository
                .findAllByUsnIgnoreCaseAndQpCodeIgnoreCaseOrderByExamYearMonthDesc(normalizedUsn, normalizedQpCode)
                .stream()
                .filter(registration -> normalizedExamYearMonth.isEmpty()
                        || normalizedExamYearMonth.equals(registration.getExamYearMonth()))
                .map(registration -> new ScannerRegistrationResponse(
                        registration.getId(),
                        registration.getUsn(),
                        registration.getQpCode(),
                        registration.getExamYearMonth(),
                        registration.getStudent().getStudentName(),
                        registration.getBooklet() != null))
                .toList();
    }

    @Transactional
    public ScannerBookletSubmitResponse submit(
            UUID registrationId, String bookletNumber, MultipartFile file) {
        String normalizedBookletNumber = requireText(bookletNumber, "Booklet number is required").toUpperCase();
        if (normalizedBookletNumber.length() > 80) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Booklet number cannot exceed 80 characters");
        }

        HallTicketData registration = hallTicketDataRepository.findById(registrationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exam registration was not found"));
        if (registration.getBooklet() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "This exam registration already has a booklet linked");
        }
        if (bookletRecordRepository.findByBookletNumberIgnoreCase(normalizedBookletNumber).isPresent()
                || scriptRepository.findByAnswerBookletId(normalizedBookletNumber).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Booklet number has already been scanned");
        }

        User scanner = currentUserService.getCurrentUser()
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
        String qpCode = registration.getQpCode().trim().toUpperCase();
        Subject subject = subjectRepository.findByQpCodeIgnoreCase(qpCode).orElse(null);

        ValuationScript script = new ValuationScript();
        script.setSubjectId(subject == null ? null : subject.getId());
        script.setAnswerBookletId(normalizedBookletNumber);
        script.setCandidateId(registration.getUsn());
        script.setCandidateName(registration.getStudent().getStudentName());
        script.setQpCode(qpCode);
        script.setSubject(subject == null ? null : subject.getSubjectName());
        script.setExamDate(YearMonth.parse(registration.getExamYearMonth()).atDay(1));
        script.setCourse(subject == null ? null : subject.getCourse());
        script.setSemester(subject == null ? null : subject.getSemester());
        script.setCenterCode(registration.getStudent().getCenterCode());
        script.setScannedFileName(normalizedBookletNumber + ".pdf");
        script.setStatus(ValuationStatus.PENDING);
        script.setCreatedBy(scanner);
        scriptRepository.saveAndFlush(script);

        BookletRecord booklet = new BookletRecord();
        booklet.setBookletNumber(normalizedBookletNumber);
        bookletRecordRepository.saveAndFlush(booklet);

        registration.setBooklet(booklet);
        hallTicketDataRepository.saveAndFlush(registration);

        storageService.storeOriginal(script.getId(), registration.getQpCode(), file);
        String originalPdfPath = SupabaseStorageService.objectPath(
                registration.getQpCode(), "qp-original", script.getId());
        booklet.setOriginalPdfUrl(originalPdfPath);
        bookletRecordRepository.save(booklet);

        script.setFileUrl("/api/files/scripts/" + script.getId());
        scriptRepository.save(script);

        return new ScannerBookletSubmitResponse(
                script.getId(), booklet.getId(), normalizedBookletNumber, originalPdfPath);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private static void validateExamYearMonth(String value) {
        try {
            YearMonth.parse(value);
        } catch (DateTimeParseException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Exam year-month must use YYYY-MM format");
        }
    }
}
