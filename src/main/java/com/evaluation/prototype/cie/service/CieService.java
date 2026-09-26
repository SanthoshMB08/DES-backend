package com.evaluation.prototype.cie.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.evaluation.prototype.auth.service.CurrentUserService;
import com.evaluation.prototype.cie.dto.CieComponentRequest;
import com.evaluation.prototype.cie.dto.CieComponentResponse;
import com.evaluation.prototype.cie.dto.CieMarkEntryRequest;
import com.evaluation.prototype.cie.dto.CieRosterRow;
import com.evaluation.prototype.cie.entity.CieComponent;
import com.evaluation.prototype.cie.entity.CieMark;
import com.evaluation.prototype.cie.repository.CieComponentRepository;
import com.evaluation.prototype.cie.repository.CieMarkRepository;
import com.evaluation.prototype.common.exception.ApiException;
import com.evaluation.prototype.student.entity.Student;
import com.evaluation.prototype.student.service.StudentService;
import com.evaluation.prototype.user.entity.User;
import com.evaluation.prototype.valuation.entity.Subject;
import com.evaluation.prototype.valuation.repository.SubjectRepository;

/**
 * Faculty (assigned to a subject by its Course's BO, parallel to how the BO
 * assigns a Chief for SEE evaluation) defines their own CIE components - Test 1,
 * Test 2, Assignment, whatever their assessment plan is - then enters marks per
 * student per component. There's no separate enrollment step: a student appears
 * on the CIE sheet the moment Faculty enters their first mark, using the same
 * find-or-create-by-register-number pattern the scanner and invigilator flows
 * already use.
 */
@Service
public class CieService {

    private final SubjectRepository subjectRepository;
    private final CieComponentRepository componentRepository;
    private final CieMarkRepository markRepository;
    private final StudentService studentService;
    private final CurrentUserService currentUserService;

    public CieService(
            SubjectRepository subjectRepository,
            CieComponentRepository componentRepository,
            CieMarkRepository markRepository,
            StudentService studentService,
            CurrentUserService currentUserService) {
        this.subjectRepository = subjectRepository;
        this.componentRepository = componentRepository;
        this.markRepository = markRepository;
        this.studentService = studentService;
        this.currentUserService = currentUserService;
    }

    private User currentUser() {
        return currentUserService.getCurrentUser()
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }

    /** The subject(s) this faculty member is assigned to teach. */
    public List<Subject> mySubjects() {
        return subjectRepository.findByFacultyId(currentUser().getId());
    }

    private Subject requireOwnSubject(UUID subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subject not found"));
        User faculty = currentUser();
        if (subject.getFaculty() == null || !subject.getFaculty().getId().equals(faculty.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not the assigned faculty for this subject");
        }
        return subject;
    }

    // ---------- Components ----------

    @Transactional
    public CieComponentResponse createComponent(UUID subjectId, CieComponentRequest request) {
        Subject subject = requireOwnSubject(subjectId);
        if (subject.isCieLocked()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CIE marks are locked for this subject");
        }
        if (componentRepository.findBySubjectIdAndNameIgnoreCase(subjectId, request.name()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "A component with this name already exists for this subject");
        }
        CieComponent component = new CieComponent();
        component.setSubject(subject);
        component.setName(request.name());
        component.setMaxMarks(request.maxMarks());
        component.setSequence(request.sequence() != null ? request.sequence() : componentRepository.findBySubjectIdOrderBySequenceAsc(subjectId).size());
        component.setCreatedBy(currentUser());
        component = componentRepository.save(component);
        return toResponse(component);
    }

    public List<CieComponentResponse> listComponents(UUID subjectId) {
        return componentRepository.findBySubjectIdOrderBySequenceAsc(subjectId).stream().map(this::toResponse).toList();
    }

    // ---------- Marks ----------

    @Transactional
    public CieRosterRow enterMark(UUID componentId, CieMarkEntryRequest request) {
        CieComponent component = componentRepository.findById(componentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Component not found"));
        requireOwnSubject(component.getSubject().getId());
        if (component.getSubject().isCieLocked()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CIE marks are locked for this subject");
        }
        if (request.marksObtained().compareTo(component.getMaxMarks()) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Marks cannot exceed this component's maximum of " + component.getMaxMarks());
        }

        Student student = studentService.findOrCreateAndLinkAttendance(request.registerNumber(), request.studentName());

        CieMark mark = markRepository.findByComponentIdAndStudentId(componentId, student.getId()).orElseGet(() -> {
            CieMark m = new CieMark();
            m.setComponent(component);
            m.setStudent(student);
            return m;
        });
        mark.setMarksObtained(request.marksObtained());
        mark.setEnteredBy(currentUser());
        markRepository.save(mark);

        return rosterRowFor(student, component.getSubject().getId());
    }

    /** The full CIE marks sheet for a subject: one row per student who has at least one mark entered. */
    public List<CieRosterRow> getRoster(UUID subjectId) {
        List<CieComponent> components = componentRepository.findBySubjectIdOrderBySequenceAsc(subjectId);
        if (components.isEmpty()) {
            return List.of();
        }
        List<UUID> componentIds = components.stream().map(CieComponent::getId).toList();
        List<CieMark> marks = markRepository.findByComponentIdIn(componentIds);

        Map<UUID, CieRosterRowBuilder> byStudent = new LinkedHashMap<>();
        for (CieMark mark : marks) {
            Student student = mark.getStudent();
            CieRosterRowBuilder builder = byStudent.computeIfAbsent(student.getId(), id -> new CieRosterRowBuilder(student));
            builder.marksByComponent.put(mark.getComponent().getId(), mark.getMarksObtained());
        }
        return byStudent.values().stream().map(CieRosterRowBuilder::build).toList();
    }

    private CieRosterRow rosterRowFor(Student student, UUID subjectId) {
        List<CieComponent> components = componentRepository.findBySubjectIdOrderBySequenceAsc(subjectId);
        List<UUID> componentIds = components.stream().map(CieComponent::getId).toList();
        List<CieMark> marks = markRepository.findByStudentIdAndComponentIdIn(student.getId(), componentIds);
        CieRosterRowBuilder builder = new CieRosterRowBuilder(student);
        marks.forEach(m -> builder.marksByComponent.put(m.getComponent().getId(), m.getMarksObtained()));
        return builder.build();
    }

    @Transactional
    public void setLocked(UUID subjectId, boolean locked) {
        Subject subject = requireOwnSubject(subjectId);
        subject.setCieLocked(locked);
        subjectRepository.save(subject);
    }

    private CieComponentResponse toResponse(CieComponent component) {
        return new CieComponentResponse(component.getId(), component.getName(), component.getMaxMarks(), component.getSequence());
    }

    /** Small local helper - accumulates one student's marks across components before building the immutable response row. */
    private static class CieRosterRowBuilder {
        final Student student;
        final Map<UUID, BigDecimal> marksByComponent = new LinkedHashMap<>();

        CieRosterRowBuilder(Student student) {
            this.student = student;
        }

        CieRosterRow build() {
            BigDecimal total = marksByComponent.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            return new CieRosterRow(student.getId(), student.getRegisterNumber(), student.getStudentName(), marksByComponent, total);
        }
    }
}
