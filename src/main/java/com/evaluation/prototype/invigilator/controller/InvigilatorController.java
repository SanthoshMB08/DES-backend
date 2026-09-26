package com.evaluation.prototype.invigilator.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.evaluation.prototype.attendance.dto.AttendanceResponse;
import com.evaluation.prototype.attendance.dto.BookletScanRequest;
import com.evaluation.prototype.attendance.dto.MarkAbsentRequest;
import com.evaluation.prototype.attendance.service.AttendanceService;
import com.evaluation.prototype.auth.service.CurrentUserService;
import com.evaluation.prototype.common.enums.AttendanceStatus;
import com.evaluation.prototype.hallticket.dto.BarcodeExamMatch;
import com.evaluation.prototype.hallticket.dto.BarcodeLookupResponse;
import com.evaluation.prototype.hallticket.dto.BarcodeScanRequest;
import com.evaluation.prototype.hallticket.dto.HallTicketScanRequest;
import com.evaluation.prototype.hallticket.dto.MobileLookupResponse;
import com.evaluation.prototype.hallticket.dto.MobileScanRequest;
import com.evaluation.prototype.hallticket.dto.MobileScanResponse;
import com.evaluation.prototype.hallticket.service.HallTicketService;
import com.evaluation.prototype.hallticket.service.MobileInvigilatorService;
import com.evaluation.prototype.student.dto.StudentResponse;
import com.evaluation.prototype.student.repository.StudentRepository;
import com.evaluation.prototype.user.entity.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/invigilator")
@PreAuthorize("hasAnyRole('ADMIN','INVIGILATOR','SUPPORT_STAFF')")
public class InvigilatorController {

    private final HallTicketService hallTicketService;
    private final MobileInvigilatorService mobileInvigilatorService;
    private final AttendanceService attendanceService;
    private final CurrentUserService currentUserService;
    private final StudentRepository studentRepository;

    public InvigilatorController(
            HallTicketService hallTicketService,
            MobileInvigilatorService mobileInvigilatorService,
            AttendanceService attendanceService,
            CurrentUserService currentUserService,
            StudentRepository studentRepository) {
        this.hallTicketService = hallTicketService;
        this.mobileInvigilatorService = mobileInvigilatorService;
        this.attendanceService = attendanceService;
        this.currentUserService = currentUserService;
        this.studentRepository = studentRepository;
    }

    // ---------- Barcode-first mobile flow (primary - needs pre-loaded hall ticket data) ----------

    /**
     * Scan the student's hall ticket barcode (register number) - this alone
     * resolves whatever exam(s) they have scheduled today, no manual
     * semester/QP-code entry. Requires hall ticket data to have been imported
     * first (see AdminHallTicketController).
     */
    @GetMapping("/mobile/lookup-by-barcode")
    public BarcodeLookupResponse lookupByBarcode(@RequestParam String registerNumber) {
        return mobileInvigilatorService.lookupByBarcode(registerNumber);
    }

    /** Second scan (the answer booklet) once the exam match is confirmed. */
    @PostMapping("/mobile/scan-by-barcode")
    public BarcodeExamMatch scanByBarcode(@Valid @RequestBody BarcodeScanRequest request) {
        User actor = currentUserService.getCurrentUser().orElse(null);
        return mobileInvigilatorService.scanByBarcode(request, actor);
    }

    // ---------- Manual mobile flow (fallback - for students with no pre-loaded hall ticket) ----------

    /** Call right after scanning the student's QR code, before showing the booklet-entry form. */
    @GetMapping("/mobile/lookup")
    public MobileLookupResponse mobileLookup(@RequestParam String registerNumber, @RequestParam String semester) {
        return mobileInvigilatorService.lookup(registerNumber, semester);
    }

    /** One call = QR scan + booklet scan for today's paper. */
    @PostMapping("/mobile/scan")
    public MobileScanResponse mobileScan(@Valid @RequestBody MobileScanRequest request) {
        User actor = currentUserService.getCurrentUser().orElse(null);
        return mobileInvigilatorService.scan(request, actor);
    }

    // ---------- Legacy / batch flow (still available) ----------

    @PostMapping("/hall-tickets/scan")
    public List<AttendanceResponse> scanHallTicket(@Valid @RequestBody HallTicketScanRequest request) {
        User actor = currentUserService.getCurrentUser().orElse(null);
        return hallTicketService.scan(request, actor).stream()
                .map(AttendanceResponse::from)
                .toList();
    }

    @PostMapping("/attendance/booklet-scan")
    public AttendanceResponse scanAnswerBooklet(@Valid @RequestBody BookletScanRequest request) {
        User actor = currentUserService.getCurrentUser().orElse(null);
        return AttendanceResponse.from(attendanceService.markPresent(request, actor));
    }

    @PostMapping("/attendance/mark-absent")
    public AttendanceResponse markAbsent(@Valid @RequestBody MarkAbsentRequest request) {
        User actor = currentUserService.getCurrentUser().orElse(null);
        return AttendanceResponse.from(attendanceService.markAbsent(request, actor));
    }

    @GetMapping("/students")
    public List<StudentResponse> searchStudents(@RequestParam String registerNumber) {
        return studentRepository.findByRegisterNumberContainingIgnoreCase(registerNumber).stream()
                .map(StudentResponse::from)
                .toList();
    }

    @GetMapping("/attendance")
    public List<AttendanceResponse> searchAttendance(
            @RequestParam(required = false) String registerNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate examDate,
            @RequestParam(required = false) String qpCode,
            @RequestParam(required = false) AttendanceStatus status) {
        return attendanceService.search(registerNumber, examDate, qpCode, status).stream()
                .map(AttendanceResponse::from)
                .toList();
    }
}
