package com.evaluation.prototype.valuation.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.evaluation.prototype.academic.dto.MarkingSection;
import com.evaluation.prototype.academic.entity.AcademicSubject;
import com.evaluation.prototype.academic.repository.AcademicSubjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.evaluation.prototype.attendance.entity.Attendance;
import com.evaluation.prototype.attendance.repository.AttendanceRepository;
import com.evaluation.prototype.auth.service.CurrentUserService;
import com.evaluation.prototype.common.exception.ApiException;
import com.evaluation.prototype.hallticket.entity.HallTicketExam;
import com.evaluation.prototype.hallticket.repository.HallTicketExamRepository;
import com.evaluation.prototype.user.entity.User;
import com.evaluation.prototype.user.repository.UserRepository;
import com.evaluation.prototype.valuation.dto.BookletLookupResponse;
import com.evaluation.prototype.valuation.dto.BundleResponse;
import com.evaluation.prototype.valuation.dto.ReviewRequest;
import com.evaluation.prototype.valuation.dto.ScriptCreateRequest;
import com.evaluation.prototype.valuation.dto.ScriptResponse;
import com.evaluation.prototype.valuation.dto.ScriptSubmitRequest;
import com.evaluation.prototype.valuation.entity.Subject;
import com.evaluation.prototype.valuation.entity.ValuationBundle;
import com.evaluation.prototype.valuation.entity.ValuationScript;
import com.evaluation.prototype.valuation.enums.SubjectStatus;
import com.evaluation.prototype.valuation.enums.ValuationStatus;
import com.evaluation.prototype.valuation.repository.SubjectRepository;
import com.evaluation.prototype.valuation.repository.ValuationBundleRepository;
import com.evaluation.prototype.valuation.repository.ValuationScriptRepository;
import com.evaluation.prototype.academic.entity.Syllabus;
import com.evaluation.prototype.valuation.entity.Course;

import org.springframework.web.multipart.MultipartFile;

import com.evaluation.prototype.storage.SupabaseStorageService;
import com.evaluation.prototype.student.entity.Student;

@Service
public class ValuationService {

    private static final Logger log = LoggerFactory.getLogger(ValuationService.class);
    private static final int BUNDLE_SIZE = 20;

    private final ValuationBundleRepository bundleRepository;
    private final ValuationScriptRepository scriptRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicSubjectRepository academicSubjectRepository;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final SupabaseStorageService fileStorageService;
    private final AttendanceRepository attendanceRepository;
    private final HallTicketExamRepository hallTicketExamRepository;

    public ValuationService(
            ValuationBundleRepository bundleRepository,
            ValuationScriptRepository scriptRepository,
            SubjectRepository subjectRepository,
            AcademicSubjectRepository academicSubjectRepository,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            SupabaseStorageService fileStorageService,
            AttendanceRepository attendanceRepository,
            HallTicketExamRepository hallTicketExamRepository) {
        this.bundleRepository = bundleRepository;
        this.scriptRepository = scriptRepository;
        this.subjectRepository = subjectRepository;
        this.academicSubjectRepository = academicSubjectRepository;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.fileStorageService = fileStorageService;
        this.attendanceRepository = attendanceRepository;
        this.hallTicketExamRepository = hallTicketExamRepository;
    }

    private User currentUser() {
        return currentUserService.getCurrentUser()
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }

    /**
     * Throws if the subject is CLOSED or PUBLISHED.
     * Called on every write operation that should be locked after evaluation closes.
     */
    private void requireSubjectOpen(UUID subjectId) {
        if (subjectId == null) return;
        subjectRepository.findById(subjectId).ifPresent(subject -> {
            if (subject.getStatus() == SubjectStatus.CLOSED) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Evaluation for this subject has been closed by the RE - no further changes allowed");
            }
            if (subject.getStatus() == SubjectStatus.PUBLISHED) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Results for this subject have been published - changes are no longer allowed");
            }
        });
    }

    /** Throws if the submitted marks exceed the subject's configured maximum. */
    private void requireWithinMaxMarks(UUID subjectId, java.math.BigDecimal totalMarks) {
        if (subjectId == null || totalMarks == null) return;
        subjectRepository.findById(subjectId).ifPresent(subject -> {
            if (subject.getMaxMarks() != null && totalMarks.compareTo(subject.getMaxMarks()) > 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Marks cannot exceed the subject's maximum of " + subject.getMaxMarks());
            }
        });
    }

    // ---------- SCANNER ----------
    // Scripts are scanned straight into an unbundled pool for their subject. The BO
    // groups them into bundles of 20 explicitly (see formBundle below).

    @Transactional
    public ScriptResponse createScript(ScriptCreateRequest request) {
        User scanner = currentUser();

        if (scriptRepository.findByAnswerBookletId(request.answerBookletId()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Answer booklet already scanned: " + request.answerBookletId());
        }

        // Scanner works independently of RE evaluation status - they can pre-scan
        // booklets before the RE opens evaluation. The subject lookup is only
        // to auto-fill course/semester/subject from the QP code.
        Subject subject = subjectRepository.findByQpCode(request.qpCode()).orElse(null);

        ValuationScript script = new ValuationScript();
        script.setSubjectId(subject != null ? subject.getId() : null);
        script.setAnswerBookletId(request.answerBookletId());
        script.setCandidateId(request.candidateId());
        script.setCandidateName(request.candidateName());
        script.setQpCode(request.qpCode());
        script.setSubject(request.subject() != null ? request.subject() : (subject != null ? subject.getSubjectName() : null));
        script.setExamDate(request.examDate());
        script.setCourse(request.course() != null ? request.course() : (subject != null ? subject.getCourse() : null));
        script.setSemester(request.semester() != null ? request.semester() : (subject != null ? subject.getSemester() : null));
        script.setCenterCode(request.centerCode());
        script.setScannedFileName(request.scannedFileName());
        script.setStatus(ValuationStatus.PENDING);
        script.setCreatedBy(scanner);
        script = scriptRepository.save(script);

        log.info("Scanned answer script {} created for subject {}",
                script.getId(), script.getSubjectId());
        return toScriptResponse(script);
    }

    public List<ScriptResponse> listMyScannedScripts() {
        User scanner = currentUser();
        return scriptRepository.findByCreatedById(scanner.getId()).stream()
                .map(this::toScriptResponse)
                .toList();
    }

    /** Scanner attaches the actual scanned PDF right after creating the script record. */
    @Transactional
    public ScriptResponse attachScannedFile(UUID scriptId, MultipartFile file) {
        ValuationScript script = findScript(scriptId);
        fileStorageService.store(scriptId, file);
        script.setFileUrl("/api/files/scripts/" + scriptId);
        if (script.getScannedFileName() == null || script.getScannedFileName().isBlank()) {
            script.setScannedFileName(file.getOriginalFilename());
        }
        script = scriptRepository.save(script);
        log.info("Scanned file attached to answer script {}", scriptId);
        return toScriptResponse(script);
    }

    /** Returns the currently stored PDF bytes for a script - the scanned original, or the
     *  evaluator's annotated copy once one has been submitted. Never a forced download. */
    public byte[] getScriptFile(UUID scriptId) {
        findScript(scriptId);
        return fileStorageService.read(scriptId);
    }

    /** Evaluator uploads their corrected/annotated copy back, overwriting the same stored file. */
    @Transactional
    public void replaceScriptFile(UUID scriptId, MultipartFile file) {
        ValuationScript script = findScript(scriptId);
        requireSubjectOpen(script.getSubjectId());
        if (script.getStatus() == ValuationStatus.REVIEWED || script.getStatus() == ValuationStatus.APPROVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This script has already been reviewed and fixed");
        }
        fileStorageService.store(scriptId, file);
        script.setFileUrl("/api/files/scripts/" + scriptId);
        scriptRepository.save(script);
    }

    // ---------- CHIEF (formerly BO) - forms bundles from scanned scripts, assigns to evaluators ----------
    // Method names still say "Bo" for minimal-risk continuity with existing callers/tests -
    // they're called via /api/chief/... now (see ChiefController). They key off
    // currentUser().getSubjectId(), which resolves correctly for whoever is authenticated.

    public List<ScriptResponse> listUnbundledScripts() {
        User bo = currentUser();
        UUID subjectId = bo.getSubjectId();
        if (subjectId == null) return List.of();
        return scriptRepository.findBySubjectIdAndBundleIsNull(subjectId).stream()
                .map(this::toScriptResponse)
                .toList();
    }

    /** Groups up to the next 20 unbundled scripts for the BO's subject into a new bundle. */
    @Transactional
    public BundleResponse formBundle() {
        User bo = currentUser();
        UUID subjectId = bo.getSubjectId();
        if (subjectId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You are not assigned to a subject");
        }
        requireSubjectOpen(subjectId);
        List<ValuationScript> pool = scriptRepository.findBySubjectIdAndBundleIsNull(subjectId);
        if (pool.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No unbundled answer sheets waiting for this subject");
        }
        List<ValuationScript> batch = pool.subList(0, Math.min(BUNDLE_SIZE, pool.size()));

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subject not found"));

        ValuationBundle bundle = new ValuationBundle();
        bundle.setBundleCode(subject.getQpCode() + "-B" + (bundleRepository.countBySubjectId(subjectId) + 1));
        bundle.setCourse(subject.getCourse());
        bundle.setSemester(subject.getSemester());
        bundle.setQpCode(subject.getQpCode());
        bundle.setSubject(subject.getSubjectName());
        bundle.setSubjectId(subjectId);
        bundle.setStatus(ValuationStatus.PENDING);
        bundle.setCreatedBy(bo);
        bundle.setTotalScripts(batch.size());
        bundle = bundleRepository.save(bundle);

        for (ValuationScript script : batch) {
            script.setBundle(bundle);
            scriptRepository.save(script);
        }

        log.info("Valuation bundle {} formed for subject {} with {} scripts",
                bundle.getId(), subjectId, batch.size());
        return toBundleResponse(bundle);
    }

    public List<BundleResponse> listBoBundles() {
        User bo = currentUser();
        if (bo.getSubjectId() == null) return List.of();
        return bundleRepository.findBySubjectId(bo.getSubjectId()).stream()
                .filter(bundle -> bundle.getTotalScripts() > 0)
                .map(this::toBundleResponse)
                .toList();
    }

    @Transactional
    public BundleResponse assignBundle(UUID bundleId, UUID evaluatorId) {
        User chief = currentUser();
        ValuationBundle bundle = bundleRepository.findById(bundleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bundle not found"));
        requireSubjectOpen(bundle.getSubjectId());
        if (bundle.getSubjectId() == null || !bundle.getSubjectId().equals(chief.getSubjectId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This bundle does not belong to your subject");
        }
        if (bundle.getValuator() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "Bundle is already assigned");
        }
        if (bundle.getTotalScripts() <= 0) {
            throw new ApiException(HttpStatus.CONFLICT, "This bundle has no remaining scripts. Form a new bundle instead.");
        }
        User evaluator = userRepository.findById(evaluatorId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Evaluator not found"));
        if (evaluator.getSubjectId() == null || !evaluator.getSubjectId().equals(bundle.getSubjectId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This evaluator does not belong to this subject");
        }
        if (!evaluator.isAvailable()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Evaluator is not available");
        }
        bundle.setValuator(evaluator);
        bundle = bundleRepository.save(bundle);

        evaluator.setAvailable(false);
        userRepository.save(evaluator);

        log.info("Valuation bundle {} assigned to evaluator {}",
                bundle.getId(), evaluator.getId());
        return toBundleResponse(bundle);
    }

    // ---------- VALUATOR (Evaluator) ----------

    public List<BundleResponse> listValuatorBundles() {
        User valuator = currentUser();
        return bundleRepository.findByValuatorId(valuator.getId()).stream()
                .filter(bundle -> bundle.getTotalScripts() > 0)
                .map(this::toBundleResponse)
                .toList();
    }

    public List<ScriptResponse> listBundleScripts(UUID bundleId) {
        ensureBundleExists(bundleId);
        return scriptRepository.findByBundleId(bundleId).stream().map(this::toScriptResponse).toList();
    }

    @Transactional
    public int submitBundle(UUID bundleId) {
        User valuator = currentUser();
        ValuationBundle bundle = bundleRepository.findById(bundleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bundle not found"));
        if (bundle.getValuator() == null || !bundle.getValuator().getId().equals(valuator.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This bundle is not assigned to you");
        }
        if (bundle.getStatus() != ValuationStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "This bundle has already been submitted");
        }
        requireSubjectOpen(bundle.getSubjectId());

        List<ValuationScript> scripts = scriptRepository.findByBundleId(bundleId);
        List<ValuationScript> remaining = scripts.stream()
                .filter(script -> script.getStatus() == ValuationStatus.PENDING)
                .toList();
        remaining.forEach(script -> script.setBundle(null));
        scriptRepository.saveAll(remaining);

        List<ValuationScript> submitted = scripts.stream()
                .filter(script -> script.getStatus() != ValuationStatus.PENDING)
                .toList();
        bundle.setTotalScripts(submitted.size());
        if (submitted.isEmpty()) {
            bundle.setValuator(null);
            bundle.setStatus(ValuationStatus.PENDING);
        } else {
            recalculateBundleStatus(bundle);
        }
        bundleRepository.save(bundle);

        valuator.setAvailable(true);
        userRepository.save(valuator);
        log.info("Valuation bundle {} submitted by evaluator {}; {} pending scripts returned",
                bundle.getId(), valuator.getId(), remaining.size());
        return remaining.size();
    }

    public ScriptResponse getScript(UUID scriptId) {
        return toScriptResponse(findScript(scriptId));
    }

    @Transactional
    public ScriptResponse submitScriptMarks(UUID scriptId, ScriptSubmitRequest request) {
        User valuator = currentUser();
        ValuationScript script = findScript(scriptId);
        requireSubjectOpen(script.getSubjectId());
        if (script.getBundle() == null || script.getBundle().getValuator() == null
                || !script.getBundle().getValuator().getId().equals(valuator.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This answer script is not assigned to you");
        }
        if (script.getStatus() == ValuationStatus.REVIEWED || script.getStatus() == ValuationStatus.APPROVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Marks are already reviewed and fixed - they can no longer be changed");
        }
        java.math.BigDecimal totalMarks = calculateConfiguredTotalMarks(
                script, request.marksJson(), request.totalMarks());
        requireWithinMaxMarks(script.getSubjectId(), totalMarks);
        script.setMarksJson(request.marksJson());
        script.setTotalMarks(totalMarks);
        script.setStatus(ValuationStatus.VALUATED);
        script = scriptRepository.save(script);

        ValuationBundle bundle = script.getBundle();
        if (bundle != null) {
            // Script submissions stay pending at bundle level until the evaluator
            // explicitly submits the whole bundle from their dashboard.
            bundle.setStatus(ValuationStatus.PENDING);
            bundleRepository.save(bundle);
        }

        log.info("Answer script {} evaluated by evaluator {}", script.getId(), valuator.getId());
        return toScriptResponse(script);
    }

    // ---------- REVIEWER ----------

    public List<BundleResponse> listReviewerBundles() {
        User reviewer = currentUser();
        return bundleRepository.findByValuator_ManagerId(reviewer.getId()).stream()
                .filter(bundle -> bundle.getStatus() != ValuationStatus.PENDING)
                .map(this::toBundleResponse)
                .toList();
    }

    @Transactional
    public ScriptResponse reviewScript(UUID scriptId, ReviewRequest request) {
        User reviewer = currentUser();
        ValuationScript script = findScript(scriptId);
        requireSubjectOpen(script.getSubjectId());
        if (script.getSubjectId() == null || !script.getSubjectId().equals(reviewer.getSubjectId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This answer script is not in your subject");
        }
        if (script.getStatus() != ValuationStatus.VALUATED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Script is not awaiting review");
        }
        if (request.marksJson() != null) {
            script.setMarksJson(request.marksJson());
        }
        if (request.marksJson() != null || request.totalMarks() != null) {
            java.math.BigDecimal totalMarks = calculateConfiguredTotalMarks(
                    script,
                    request.marksJson() == null ? script.getMarksJson() : request.marksJson(),
                    request.totalMarks() == null ? script.getTotalMarks() : request.totalMarks());
            if (totalMarks != null) {
                requireWithinMaxMarks(script.getSubjectId(), totalMarks);
                script.setTotalMarks(totalMarks);
            }
        }
        script.setReviewComments(request.comments());
        // Once approved the marks are fixed (REVIEWED is the final, locked state in this flow).
        script.setStatus(request.approved() ? ValuationStatus.REVIEWED : ValuationStatus.PENDING);
        script = scriptRepository.save(script);

        ValuationBundle bundle = script.getBundle();
        if (bundle != null) {
            if (bundle.getReviewer() == null) {
                bundle.setReviewer(reviewer);
            }
            recalculateBundleStatus(bundle);
            bundleRepository.save(bundle);
        }

        log.info("Answer script {} review completed by reviewer {}; approved={}, status={}",
                script.getId(), reviewer.getId(), request.approved(), script.getStatus());
        return toScriptResponse(script);
    }

    // ---------- SHARED ----------

    private void ensureBundleExists(UUID bundleId) {
        if (!bundleRepository.existsById(bundleId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Bundle not found");
        }
    }

    private ValuationScript findScript(UUID scriptId) {
        return scriptRepository.findById(scriptId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Script not found"));
    }

    private void recalculateBundleStatus(ValuationBundle bundle) {
        List<ValuationScript> scripts = scriptRepository.findByBundleId(bundle.getId());
        if (scripts.isEmpty()) {
            return;
        }
        boolean allApproved = scripts.stream().allMatch(s -> s.getStatus() == ValuationStatus.APPROVED);
        boolean allReviewed = scripts.stream().allMatch(s -> s.getStatus() == ValuationStatus.REVIEWED || s.getStatus() == ValuationStatus.APPROVED);
        boolean allValuated = scripts.stream().allMatch(s -> s.getStatus() == ValuationStatus.VALUATED
                || s.getStatus() == ValuationStatus.REVIEWED || s.getStatus() == ValuationStatus.APPROVED);

        if (allApproved) {
            bundle.setStatus(ValuationStatus.APPROVED);
        } else if (allReviewed) {
            bundle.setStatus(ValuationStatus.REVIEWED);
        } else if (allValuated) {
            bundle.setStatus(ValuationStatus.VALUATED);
        } else {
            bundle.setStatus(ValuationStatus.PENDING);
        }
    }

    @Transactional(readOnly = true)
    public BookletLookupResponse lookupBooklet(String bookletId) {

        Attendance attendance = attendanceRepository
                .findByAnswerBookletNumber(bookletId)
                .orElseThrow(() ->
                    new ApiException(HttpStatus.NOT_FOUND, "Answer booklet not found: " + bookletId));

        Student student = attendance.getStudent();
        Subject subject = subjectRepository.findByQpCode(attendance.getQpCode()).orElse(null);

        // The hall-ticket exam row tells us which paper, date and centre this
        // booklet was actually issued for. The evaluation subject supplies the
        // degree-level course/semester, so it stays the primary source for those
        // - on a hall-ticket exam row "course" means the paper name, not the
        // degree, and must not overwrite it.
        String registerNumber = student != null ? student.getRegisterNumber() : null;
        HallTicketExam exam = (registerNumber == null || attendance.getExamDate() == null)
                ? null
                : hallTicketExamRepository
                        .findByHallTicket_RegisterNumberIgnoreCaseAndExamDateAndQpCode(
                                registerNumber, attendance.getExamDate(), attendance.getQpCode())
                        .orElse(null);

        String semesterText = subject != null ? subject.getSemester() : null;
        if (semesterText == null && exam != null && exam.getHallTicket() != null) {
            semesterText = exam.getHallTicket().getSemester();
        }
        Integer semesterNumber = null;
        if (semesterText != null) {
            try {
                semesterNumber = Integer.valueOf(semesterText.trim());
            } catch (NumberFormatException e) {
                semesterNumber = null;
            }
        }

        return new BookletLookupResponse(
                attendance.getAnswerBookletNumber(),
                registerNumber,
                student != null ? student.getStudentName() : null,
                attendance.getQpCode(),
                firstNonBlank(
                        subject != null ? subject.getCourse() : null,
                        exam != null ? exam.getCourseName() : null),
                semesterNumber,
                firstNonBlank(
                        subject != null ? subject.getSubjectName() : null,
                        exam != null ? exam.getCourseName() : null),
                attendance.getExamDate(),
                firstNonBlank(
                        exam != null ? exam.getCentreCode() : null,
                        student != null ? student.getCenterCode() : null),
                true
        );
    }

    /** Returns the first non-blank value, trimmed; null when all are null/blank. */
    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private BundleResponse toBundleResponse(ValuationBundle bundle) {
        return new BundleResponse(
                bundle.getId(),
                bundle.getBundleCode(),
                bundle.getCourse(),
                bundle.getSemester(),
                bundle.getQpCode(),
                bundle.getSubject(),
                bundle.getCenterCode(),
                bundle.getTotalScripts(),
                bundle.getStatus().name(),
                bundle.getValuator() == null ? null : bundle.getValuator().getFullName(),
                bundle.getReviewer() == null ? null : bundle.getReviewer().getFullName(),
                bundle.getBoe() == null ? null : bundle.getBoe().getFullName(),
                bundle.getCreatedAt());
    }

    private ScriptResponse toScriptResponse(ValuationScript script) {
        List<MarkingSection> markingScheme = findMarkingScheme(script);
        return new ScriptResponse(
                script.getId(),
                script.getBundle() == null ? null : script.getBundle().getId(),
                script.getBundle() == null ? null : script.getBundle().getBundleCode(),
                script.getAnswerBookletId(),
                script.getCandidateId(),
                script.getCandidateName(),
                script.getQpCode(),
                script.getSubject(),
                script.getExamDate(),
                script.getCourse(),
                script.getSemester(),
                script.getCenterCode(),
                script.getScannedFileName(),
                script.getFileUrl(),
                script.getStatus().name(),
                script.getMarksJson(),
                script.getTotalMarks(),
                script.getReviewComments(),
                markingScheme);
    }

    private List<MarkingSection> findMarkingScheme(ValuationScript script) {
        if (script.getQpCode() == null || script.getSemester() == null) {
            return List.of();
        }
        if (script.getSubjectId() != null) {
            Subject evaluationSubject = subjectRepository.findById(script.getSubjectId()).orElse(null);
            Course course = evaluationSubject == null ? null : evaluationSubject.getCourseRef();
            Syllabus syllabus = course == null ? null : course.getSyllabus();
            if (syllabus != null) {
                return academicSubjectRepository
                        .findBySyllabusIdAndSemesterOrderBySubjectCodeAsc(syllabus.getId(), script.getSemester())
                        .stream()
                        .filter(subject -> subject.getQpCode().equalsIgnoreCase(script.getQpCode()))
                        .map(AcademicSubject::getMarkingScheme)
                        .filter(scheme -> !scheme.isEmpty())
                        .findFirst()
                        .orElse(List.of());
            }
        }
        return academicSubjectRepository
                .findByQpCodeIgnoreCaseAndSemesterOrderByUpdatedAtDesc(script.getQpCode(), script.getSemester())
                .stream()
                .map(AcademicSubject::getMarkingScheme)
                .filter(scheme -> !scheme.isEmpty())
                .findFirst()
                .orElse(List.of());
    }

    private java.math.BigDecimal calculateConfiguredTotalMarks(
            ValuationScript script, String marksJson, java.math.BigDecimal fallbackTotal) {
        List<MarkingSection> scheme = findMarkingScheme(script);
        if (scheme.isEmpty()) {
            return fallbackTotal;
        }
        try {
            JsonNode questions = objectMapper.readTree(marksJson);
            java.math.BigDecimal calculatedTotal = MarkingSchemeScorer.calculate(questions, scheme);
            log.debug("Applied configured marking scheme to script {} across {} sections",
                    script.getId(), scheme.size());
            return calculatedTotal;
        } catch (com.evaluation.prototype.common.exception.ApiException exception) {
            log.warn("Marking scheme rejected submitted scores for script {}: {}",
                    script.getId(), exception.getMessage());
            throw exception;
        } catch (Exception exception) {
            log.warn("Could not parse submitted marks for script {}", script.getId(), exception);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Submitted question marks are invalid");
        }
    }
}
