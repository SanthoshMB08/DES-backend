package com.evaluation.prototype.valuation.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.evaluation.prototype.auth.service.CurrentUserService;
import com.evaluation.prototype.common.exception.ApiException;
import com.evaluation.prototype.user.entity.User;
import com.evaluation.prototype.user.repository.UserRepository;
import com.evaluation.prototype.valuation.dto.CreateSubjectRequest;
import com.evaluation.prototype.valuation.dto.SubjectProgressResponse;
import com.evaluation.prototype.valuation.dto.SubjectResponse;
import com.evaluation.prototype.valuation.entity.Course;
import com.evaluation.prototype.valuation.entity.Subject;
import com.evaluation.prototype.valuation.entity.ValuationScript;
import com.evaluation.prototype.valuation.enums.SubjectStatus;
import com.evaluation.prototype.valuation.enums.ValuationStatus;
import com.evaluation.prototype.valuation.repository.CourseRepository;
import com.evaluation.prototype.valuation.repository.SubjectRepository;
import com.evaluation.prototype.valuation.repository.ValuationScriptRepository;

/**
 * RE opens subjects and releases a course's results after the BO completes every subject.
 * BO completes a subject once every script has been reviewed.
 */
@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final ValuationScriptRepository scriptRepository;

    public SubjectService(
            SubjectRepository subjectRepository,
            CourseRepository courseRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            ValuationScriptRepository scriptRepository) {
        this.subjectRepository = subjectRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.scriptRepository = scriptRepository;
    }

    private User currentUser() {
        return currentUserService.getCurrentUser()
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }

    private Subject findSubject(UUID subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subject not found"));
    }

    private Course findCourse(UUID courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course not found"));
    }

    @Transactional
    public SubjectResponse createSubject(UUID courseId, CreateSubjectRequest request) {
        User re = currentUser();
        Course course = findCourse(courseId);
        if (subjectRepository.findByQpCode(request.qpCode()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "A subject with this QP code already exists");
        }
        Subject subject = new Subject();
        subject.setSubjectCode(request.subjectCode());
        subject.setSubjectName(request.subjectName());
        subject.setCourse(course.getCourseName());
        subject.setSemester(course.getSemester());
        subject.setCourseRef(course);
        subject.setQpCode(request.qpCode());
        subject.setStatus(SubjectStatus.OPEN);
        subject.setMaxMarks(request.maxMarks() != null ? request.maxMarks() : java.math.BigDecimal.valueOf(100));
        subject.setCredits(request.credits());
        subject.setOpenedBy(re);
        subject = subjectRepository.save(subject);
        return toResponse(subject);
    }

    public List<SubjectResponse> listSubjects() {
        return subjectRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<SubjectResponse> listSubjectsForCourse(UUID courseId) {
        return subjectRepository.findByCourseRefId(courseId).stream().map(this::toResponse).toList();
    }

    /**
     * Close evaluation - only allowed once every scanned script for the subject has been
     * reviewed. Deactivates the login of the Chief and every Reviewer/Evaluator created for
     * that subject, so nobody can touch scripts after the fact. The Course's BO oversees
     * multiple subjects and is intentionally left active.
     */
    @Transactional
    public SubjectResponse completeSubjectForBo(UUID subjectId) {
        User bo = currentUser();
        Subject subject = findSubject(subjectId);
        Course course = subject.getCourseRef();
        if (course == null || course.getBo() == null || !course.getBo().getId().equals(bo.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This subject is not in your course");
        }
        return closeSubject(subject);
    }

    private SubjectResponse closeSubject(Subject subject) {
        UUID subjectId = subject.getId();
        if (subject.getStatus() != SubjectStatus.OPEN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Subject is not in OPEN state");
        }
        List<ValuationScript> scripts = scriptRepository.findBySubjectId(subjectId);
        long reviewed = scripts.stream()
                .filter(s -> s.getStatus() == ValuationStatus.REVIEWED || s.getStatus() == ValuationStatus.APPROVED)
                .count();
        if (scripts.isEmpty() || reviewed < scripts.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "All answer scripts must be evaluated and reviewed before closing this subject");
        }

        subject.setStatus(SubjectStatus.CLOSED);
        subject = subjectRepository.save(subject);

        // Deactivate the Chief and everyone under them (Reviewers, Evaluators) for this QP code.
        List<User> subjectUsers = userRepository.findBySubjectId(subjectId);
        subjectUsers.forEach(User::lock);
        userRepository.saveAll(subjectUsers);

        return toResponse(subject);
    }

    /** Publish marks - makes them visible in the /marks public endpoint. */
    @Transactional
    public SubjectResponse publishSubject(UUID subjectId) {
        Subject subject = findSubject(subjectId);
        if (subject.getStatus() != SubjectStatus.CLOSED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Subject must be CLOSED before publishing marks");
        }
        subject.setStatus(SubjectStatus.PUBLISHED);
        subject = subjectRepository.save(subject);
        return toResponse(subject);
    }

    /** Publish every BO-completed subject in a course as one RE-approved release. */
    @Transactional
    public List<SubjectResponse> releaseCourse(UUID courseId) {
        findCourse(courseId);
        List<Subject> subjects = subjectRepository.findByCourseRefId(courseId);
        if (subjects.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This course has no subjects to release");
        }
        if (subjects.stream().anyMatch(subject -> subject.getStatus() == SubjectStatus.OPEN)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "The BO must complete every subject before results can be released");
        }
        subjects.stream()
                .filter(subject -> subject.getStatus() == SubjectStatus.CLOSED)
                .forEach(subject -> subject.setStatus(SubjectStatus.PUBLISHED));
        return subjectRepository.saveAll(subjects).stream().map(this::toResponse).toList();
    }

    /** Get progress for a single subject (total scripts, valuated, reviewed). */
    public SubjectProgressResponse getSubjectProgress(UUID subjectId) {
        Subject subject = findSubject(subjectId);
        List<ValuationScript> scripts = scriptRepository.findBySubjectId(subjectId);
        int total = scripts.size();
        int valuated = (int) scripts.stream()
                .filter(s -> s.getStatus() == ValuationStatus.VALUATED
                        || s.getStatus() == ValuationStatus.REVIEWED
                        || s.getStatus() == ValuationStatus.APPROVED)
                .count();
        int reviewed = (int) scripts.stream()
                .filter(s -> s.getStatus() == ValuationStatus.REVIEWED
                        || s.getStatus() == ValuationStatus.APPROVED)
                .count();
        return new SubjectProgressResponse(
                subject.getId(), subject.getSubjectCode(), subject.getSubjectName(),
                subject.getCourse(), subject.getSemester(), subject.getQpCode(),
                subject.getStatus().name(),
                subject.getChief() == null ? null : subject.getChief().getFullName(),
                subject.getChief() == null ? null : subject.getChief().getId(),
                total, valuated, reviewed);
    }

    private SubjectResponse toResponse(Subject subject) {
        return new SubjectResponse(
                subject.getId(),
                subject.getSubjectCode(),
                subject.getSubjectName(),
                subject.getCourse(),
                subject.getSemester(),
                subject.getQpCode(),
                subject.getStatus().name(),
                subject.getMaxMarks(),
                subject.getCredits(),
                subject.getCourseRef() == null ? null : subject.getCourseRef().getId(),
                subject.getChief() == null ? null : subject.getChief().getFullName(),
                subject.getChief() == null ? null : subject.getChief().getId(),
                subject.getOpenedBy().getFullName(),
                subject.getCreatedAt());
    }
}
