package com.evaluation.prototype.hallticket.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.evaluation.prototype.attendance.entity.Attendance;
import com.evaluation.prototype.attendance.repository.AttendanceRepository;
import com.evaluation.prototype.audit.service.AuditService;
import com.evaluation.prototype.booklet.entity.AnswerBooklet;
import com.evaluation.prototype.booklet.repository.AnswerBookletRepository;
import com.evaluation.prototype.common.enums.AnswerBookletStatus;
import com.evaluation.prototype.common.enums.AttendanceSource;
import com.evaluation.prototype.common.enums.AttendanceStatus;
import com.evaluation.prototype.common.exception.ApiException;
import com.evaluation.prototype.hallticket.dto.BarcodeExamMatch;
import com.evaluation.prototype.hallticket.dto.BarcodeLookupResponse;
import com.evaluation.prototype.hallticket.dto.BarcodeScanRequest;
import com.evaluation.prototype.hallticket.dto.MobileExamEntry;
import com.evaluation.prototype.hallticket.dto.MobileLookupResponse;
import com.evaluation.prototype.hallticket.dto.MobileScanRequest;
import com.evaluation.prototype.hallticket.dto.MobileScanResponse;
import com.evaluation.prototype.hallticket.entity.HallTicket;
import com.evaluation.prototype.hallticket.entity.HallTicketExam;
import com.evaluation.prototype.hallticket.repository.HallTicketExamRepository;
import com.evaluation.prototype.hallticket.repository.HallTicketRepository;
import com.evaluation.prototype.student.entity.Student;
import com.evaluation.prototype.student.repository.StudentRepository;
import com.evaluation.prototype.user.entity.User;

/**
 * Backs the mobile invigilator screen. One QR scan (register number) + one
 * booklet scan = one exam attendance marked, per the following rule:
 * <p>
 * - New (registerNumber, semester) combination -&gt; creates the Student (if
 * new), a new HallTicket for that semester, its first HallTicketExam, and
 * a PRESENT Attendance row. Requires studentName + studentType.
 * <p>
 * - Same (registerNumber, semester) seen before, new qpCode/examDate for
 * that semester -&gt; reuses the existing HallTicket, only adds the new
 * HallTicketExam + Attendance row (booklet number). Does not require
 * studentName/studentType again.
 * <p>
 * - A different semester for the same register number (e.g. a backlog
 * paper) -&gt; always a brand-new HallTicket, since HallTicket is keyed by
 * (registerNumber, semester).
 */
@Service
public class MobileInvigilatorService {

    private final StudentRepository studentRepository;
    private final HallTicketRepository hallTicketRepository;
    private final HallTicketExamRepository hallTicketExamRepository;
    private final AttendanceRepository attendanceRepository;
    private final AnswerBookletRepository answerBookletRepository;
    private final AuditService auditService;

    public MobileInvigilatorService(
            StudentRepository studentRepository,
            HallTicketRepository hallTicketRepository,
            HallTicketExamRepository hallTicketExamRepository,
            AttendanceRepository attendanceRepository,
            AnswerBookletRepository answerBookletRepository,
            AuditService auditService) {
        this.studentRepository = studentRepository;
        this.hallTicketRepository = hallTicketRepository;
        this.hallTicketExamRepository = hallTicketExamRepository;
        this.attendanceRepository = attendanceRepository;
        this.answerBookletRepository = answerBookletRepository;
        this.auditService = auditService;
    }

    /**
     * Barcode-first flow: the invigilator scans only the student's hall ticket
     * barcode (which encodes their register number - see the ocr-module repo).
     * No semester or QP code is asked for upfront - this resolves whatever
     * exam(s) this student has scheduled for TODAY across all their hall
     * tickets, using the pre-loaded data from HallTicketImportService. This is
     * what makes it safe for two different students to be writing two
     * different papers on the same day without the invigilator tracking that
     * manually.
     */
    @Transactional(readOnly = true)
    public BarcodeLookupResponse lookupByBarcode(String registerNumber) {
        String regNo = normalize(registerNumber);
        List<HallTicketExam> todaysExams = hallTicketExamRepository
                .findByHallTicket_RegisterNumberIgnoreCaseAndExamDate(regNo, LocalDate.now());

        if (todaysExams.isEmpty()) {
            // Distinguish "we've never heard of this student" from "we know them,
            // just nothing scheduled today" - the invigilator UI can word these
            // differently (the first suggests a barcode misread or missing import).
            String studentName = studentRepository.findByRegisterNumber(regNo)
                    .map(Student::getStudentName).orElse(null);
            return new BarcodeLookupResponse(studentName != null, regNo, studentName, List.of());
        }

        String studentName = todaysExams.get(0).getHallTicket().getStudent().getStudentName();
        List<BarcodeExamMatch> matches = todaysExams.stream().map(this::toBarcodeMatch).toList();
        return new BarcodeLookupResponse(true, regNo, studentName, matches);
    }

    /** Second half of the barcode-first flow - the invigilator has confirmed which exam, now scans the booklet. */
    @Transactional
    public BarcodeExamMatch scanByBarcode(BarcodeScanRequest request, User actor) {
        String regNo = normalize(request.registerNumber());
        String bookletNumber = normalize(request.answerBookletNumber());

        HallTicketExam exam = hallTicketExamRepository.findById(request.hallTicketExamId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "That exam entry no longer exists - please scan the barcode again"));

        if (!exam.getHallTicket().getRegisterNumber().equalsIgnoreCase(regNo)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This exam entry does not belong to that register number");
        }

        HallTicket hallTicket = exam.getHallTicket();
        markPresent(hallTicket.getStudent(), regNo, exam.getExamDate(), exam.getQpCode(), bookletNumber, actor);

        auditService.record(actor, "HallTicketExam", exam.getId(), "MOBILE_BARCODE_SCAN", null,
                "{\"registerNumber\":\"%s\",\"qpCode\":\"%s\",\"examDate\":\"%s\"}"
                        .formatted(regNo, exam.getQpCode(), exam.getExamDate()));

        return toBarcodeMatch(exam);
    }

    private BarcodeExamMatch toBarcodeMatch(HallTicketExam exam) {
        var attendance = attendanceRepository.findByRegisterNumberAndExamDateAndQpCode(
                exam.getHallTicket().getRegisterNumber(), exam.getExamDate(), exam.getQpCode());
        String status = attendance.map(a -> a.getStatus().name()).orElse("PENDING");
        String bookletNumber = attendance.map(Attendance::getAnswerBookletNumber).orElse(null);
        return new BarcodeExamMatch(
                exam.getId(), exam.getHallTicket().getSemester(), exam.getExamDate(), exam.getQpCode(),
                exam.getCourseName(), exam.getExamTime(), exam.getCentreCode(), status, bookletNumber);
    }

    /** Called right after the QR scan, before showing the booklet-entry form. */
    @Transactional(readOnly = true)
    public MobileLookupResponse lookup(String registerNumber, String semester) {
        String regNo = normalize(registerNumber);
        String sem = normalize(semester);

        return hallTicketRepository.findByRegisterNumberAndSemesterIgnoreCase(regNo, sem)
                .map(ticket -> new MobileLookupResponse(
                        true, regNo, sem, ticket.getStudent().getStudentName(), ticket.getStudentType(),
                        buildExamEntries(ticket)))
                .orElseGet(() -> MobileLookupResponse.notFound(regNo, sem));
    }

    @Transactional
    public MobileScanResponse scan(MobileScanRequest request, User actor) {
        String regNo = normalize(request.registerNumber());
        String semester = normalize(request.semester());
        String qpCode = normalize(request.qpCode());
        String bookletNumber = normalize(request.answerBookletNumber());
        LocalDate examDate = request.examDate() != null ? request.examDate() : LocalDate.now();

        var existingTicket = hallTicketRepository.findByRegisterNumberAndSemesterIgnoreCase(regNo, semester);
        String resultType;
        HallTicket hallTicket;
        HallTicket temp=null;

        if (existingTicket.isPresent()) {
            temp = existingTicket.get();
            boolean examAlreadyExists = hallTicketExamRepository
                    .findByHallTicket_IdAndExamDateAndQpCode(temp.getId(), examDate, qpCode)
                    .isPresent();
            resultType = examAlreadyExists ? "ALREADY_RECORDED" : "NEW_EXAM_ENTRY";
        } else {
            if (!StringUtils.hasText(request.studentName()) || request.studentType() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "This is a new hall ticket for semester " + semester + " - student name and student type are required");
            }
            Student student = studentRepository.findByRegisterNumber(regNo)
                    .orElseGet(() -> {
                        Student s = new Student();
                        s.setRegisterNumber(regNo);
                        s.setStudentName(request.studentName().trim());
                        return studentRepository.save(s);
                    });
            temp = new HallTicket();
            temp.setStudent(student);
            temp.setRegisterNumber(regNo);
            temp.setSemester(semester);
            temp.setStudentType(request.studentType());
            temp.setScannedBy(actor);
            temp = hallTicketRepository.save(temp);
            resultType = "NEW_HALL_TICKET";
        }
        hallTicket = temp;  
        HallTicketExam exam = hallTicketExamRepository
                .findByHallTicket_IdAndExamDateAndQpCode(hallTicket.getId(), examDate, qpCode)
                .orElseGet(() -> {
                    HallTicketExam e = new HallTicketExam();
                    e.setHallTicket(hallTicket);
                    e.setExamDate(examDate);
                    e.setQpCode(qpCode);
                    return hallTicketExamRepository.save(e);
                });

        markPresent(hallTicket.getStudent(), regNo, examDate, qpCode, bookletNumber, actor);

        auditService.record(actor, "HallTicket", hallTicket.getId(), "MOBILE_SCAN", null,
                "{\"registerNumber\":\"%s\",\"semester\":\"%s\",\"qpCode\":\"%s\",\"examDate\":\"%s\",\"resultType\":\"%s\"}"
                        .formatted(regNo, semester, qpCode, examDate, resultType));

        return new MobileScanResponse(
                resultType, regNo, semester, hallTicket.getStudent().getStudentName(), hallTicket.getStudentType(),
                buildExamEntries(hallTicket));
    }

    private void markPresent(Student student, String registerNumber, LocalDate examDate, String qpCode, String bookletNumber, User actor) {
        Attendance attendance = attendanceRepository.findByRegisterNumberAndExamDateAndQpCode(registerNumber, examDate, qpCode)
                .orElseGet(() -> {
                    Attendance a = new Attendance();
                    a.setStudent(student);
                    a.setRegisterNumber(registerNumber);
                    a.setExamDate(examDate);
                    a.setQpCode(qpCode);
                    a.setStatus(AttendanceStatus.PENDING);
                    a.setSource(AttendanceSource.HALL_TICKET_SCAN);
                    return a;
                });

        if (attendance.getStudent() == null) {
            attendance.setStudent(student);
        }

        if (attendance.getStatus() == AttendanceStatus.PRESENT) {
            if (bookletNumber.equals(attendance.getAnswerBookletNumber())) {
                return; // same scan retried (e.g. network retry) - no-op
            }
            throw new ApiException(HttpStatus.CONFLICT,
                    "This paper is already marked PRESENT for this student with a different booklet number ("
                            + attendance.getAnswerBookletNumber() + ")");
        }

        if (attendanceRepository.existsByAnswerBookletNumber(bookletNumber)
                && (attendance.getId() == null || attendanceRepository.existsByAnswerBookletNumberAndIdNot(bookletNumber, attendance.getId()))) {
            throw new ApiException(HttpStatus.CONFLICT, "Answer booklet number " + bookletNumber + " is already assigned to another student");
        }

        attendance.setAnswerBookletNumber(bookletNumber);
        attendance.setStatus(AttendanceStatus.PRESENT);
        attendance.setSource(AttendanceSource.BOOKLET_SCAN);
        attendance.setMarkedBy(actor);
        attendance.setMarkedAt(Instant.now());
        attendance = attendanceRepository.save(attendance);

        AnswerBooklet booklet = answerBookletRepository.findByBookletNumber(bookletNumber)
                .orElseGet(() -> {
                    AnswerBooklet b = new AnswerBooklet();
                    b.setBookletNumber(bookletNumber);
                    b.setStatus(AnswerBookletStatus.AVAILABLE);
                    return b;
                });
        booklet.assignTo(attendance, actor);
        answerBookletRepository.save(booklet);
    }

    private List<MobileExamEntry> buildExamEntries(HallTicket hallTicket) {
        return hallTicketExamRepository.findByHallTicket_IdOrderByExamDateAsc(hallTicket.getId()).stream()
                .map(exam -> {
                    var attendance = attendanceRepository
                            .findByRegisterNumberAndExamDateAndQpCode(hallTicket.getRegisterNumber(), exam.getExamDate(), exam.getQpCode());
                    String status = attendance.map(a -> a.getStatus().name()).orElse("PENDING");
                    String bookletNumber = attendance.map(Attendance::getAnswerBookletNumber).orElse(null);
                    return new MobileExamEntry(exam.getExamDate(), exam.getQpCode(), status, bookletNumber);
                })
                .toList();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
