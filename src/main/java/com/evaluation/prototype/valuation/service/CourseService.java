package com.evaluation.prototype.valuation.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.evaluation.prototype.auth.service.AuthService;
import com.evaluation.prototype.auth.service.CurrentUserService;
import com.evaluation.prototype.academic.repository.ProgramRepository;
import com.evaluation.prototype.academic.entity.Syllabus;
import com.evaluation.prototype.academic.entity.AcademicSubject;
import com.evaluation.prototype.academic.repository.SyllabusRepository;
import com.evaluation.prototype.academic.repository.AcademicSubjectRepository;
import com.evaluation.prototype.academic.repository.AcademicCurriculumRepository;
import com.evaluation.prototype.academic.entity.AcademicCurriculum;
import com.evaluation.prototype.common.exception.ApiException;
import com.evaluation.prototype.user.entity.User;
import com.evaluation.prototype.user.repository.RoleRepository;
import com.evaluation.prototype.user.repository.UserRepository;
import com.evaluation.prototype.valuation.dto.CourseResponse;
import com.evaluation.prototype.valuation.dto.CreateSubordinateRequest;
import com.evaluation.prototype.valuation.dto.OpenCourseRequest;
import com.evaluation.prototype.valuation.dto.SubjectProgressResponse;
import com.evaluation.prototype.valuation.dto.SubordinateResponse;
import com.evaluation.prototype.valuation.entity.Course;
import com.evaluation.prototype.valuation.entity.Subject;
import com.evaluation.prototype.valuation.entity.ValuationScript;
import com.evaluation.prototype.valuation.enums.ValuationStatus;
import com.evaluation.prototype.valuation.repository.CourseRepository;
import com.evaluation.prototype.valuation.repository.SubjectRepository;
import com.evaluation.prototype.valuation.repository.ValuationScriptRepository;

/**
 * RE (Register of Evaluation): creates Courses and appoints one Board Officer (BO) to each.
 * BO: oversees every Subject within their own Course - sees the status of every paper for
 * every subject at a glance, and appoints a Subject Chief to run each subject's evaluation.
 */
@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;
    private final ValuationScriptRepository scriptRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final AuthService authService;
    private final ProgramRepository programRepository;
    private final SyllabusRepository syllabusRepository;
    private final AcademicSubjectRepository academicSubjectRepository;
    private final AcademicCurriculumRepository academicCurriculumRepository;

    public CourseService(
            CourseRepository courseRepository,
            SubjectRepository subjectRepository,
            ValuationScriptRepository scriptRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            CurrentUserService currentUserService,
            AuthService authService,
            ProgramRepository programRepository,
            SyllabusRepository syllabusRepository,
            AcademicSubjectRepository academicSubjectRepository,
            AcademicCurriculumRepository academicCurriculumRepository) {
        this.courseRepository = courseRepository;
        this.subjectRepository = subjectRepository;
        this.scriptRepository = scriptRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
        this.authService = authService;
        this.programRepository = programRepository;
        this.syllabusRepository = syllabusRepository;
        this.academicSubjectRepository = academicSubjectRepository;
        this.academicCurriculumRepository = academicCurriculumRepository;
    }

    private User currentUser() {
        return currentUserService.getCurrentUser()
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }

    // ---------- RE ----------

    @Transactional
    public CourseResponse createCourse(OpenCourseRequest request) {
        User re = currentUser();
        if (courseRepository.findByCourseNameIgnoreCaseAndSemesterAndSyllabusYearIgnoreCase(
                request.courseName(), request.semester(), request.syllabusYear()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This course, semester, and syllabus year already exists");
        }
        Course course = new Course();
        course.setCourseName(request.courseName());
        course.setSemester(request.semester());
        course.setSyllabusYear(request.syllabusYear());
        if (request.programId() != null) {
            course.setProgram(programRepository.findById(request.programId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Program not found")));
        }
        if (request.syllabusId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select an academic syllabus");
        }
        Syllabus syllabus = syllabusRepository.findById(request.syllabusId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Select a valid academic syllabus"));
        if (course.getProgram() == null
                || (syllabus.getProgram() != null && !syllabus.getProgram().getId().equals(course.getProgram().getId()))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Syllabus does not belong to the selected course");
        }
        AcademicCurriculum curriculum = academicCurriculumRepository
                .findByProgramIdAndSyllabusIdAndSemester(course.getProgram().getId(), syllabus.getId(), request.semester())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                        "No curriculum is configured for this program, syllabus, and semester"));
        List<AcademicSubject> academicSubjects = academicSubjectRepository
                .findByCurriculumIdOrderBySubjectCodeAsc(curriculum.getId());
        if (academicSubjects.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "No subjects are configured for this syllabus and semester");
        }
        course.setSyllabus(syllabus);
        course.setCreatedBy(re);
        course = courseRepository.save(course);
        for (AcademicSubject master : academicSubjects) {
            Subject subject = new Subject();
            subject.setSubjectCode(master.getSubjectCode());
            subject.setSubjectName(master.getSubjectName());
            subject.setCourse(course.getCourseName());
            subject.setSemester(course.getSemester());
            subject.setCourseRef(course);
            subject.setQpCode(master.getQpCode());
            subject.setStatus(com.evaluation.prototype.valuation.enums.SubjectStatus.OPEN);
            subject.setMaxMarks(master.getMaxMarks());
            subject.setCredits(master.getCredits());
            subject.setOpenedBy(re);
            subjectRepository.save(subject);
        }
        return toResponse(course);
    }

    public List<CourseResponse> listCourses() {
        return courseRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public SubordinateResponse assignBo(UUID courseId, CreateSubordinateRequest request) {
        Course course = findCourse(courseId);
        if (course.getBo() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "This course already has a Board Officer");
        }
        User bo = createUser(request, "ROLE_BOE", currentUser());
        course.setBo(bo);
        courseRepository.save(course);
        return new SubordinateResponse(bo.getId(), bo.getUsername(), bo.getFullName(), "BOE", bo.isAvailable(), null, null);
    }

    // ---------- BO ----------

    public CourseResponse getMyCourse() {
        User bo = currentUser();
        Course course = courseRepository.findByBoId(bo.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "You have not been assigned a course yet"));
        return toResponse(course);
    }

    /** BO's view of every paper's status, grouped by subject, across their whole course. */
    public List<SubjectProgressResponse> listSubjectsForMyCourse() {
        User bo = currentUser();
        Course course = courseRepository.findByBoId(bo.getId()).orElse(null);
        if (course == null) return List.of();
        return subjectRepository.findByCourseRefId(course.getId()).stream()
                .map(this::toProgressResponse)
                .toList();
    }

    @Transactional
    public SubordinateResponse assignChief(UUID subjectId, CreateSubordinateRequest request) {
        User bo = currentUser();
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subject not found"));
        Course course = courseRepository.findByBoId(bo.getId()).orElse(null);
        if (course == null || subject.getCourseRef() == null || !subject.getCourseRef().getId().equals(course.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This subject is not in your course");
        }
        if (subject.getChief() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "This subject already has a Chief");
        }
        User chief = createUser(request, "ROLE_CHIEF", bo);
        chief.setSubjectId(subjectId);
        chief = userRepository.save(chief);
        subject.setChief(chief);
        subjectRepository.save(subject);
        return new SubordinateResponse(chief.getId(), chief.getUsername(), chief.getFullName(), "CHIEF", chief.isAvailable(), subjectId, null);
    }

    // ---------- shared ----------

    private Course findCourse(UUID courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course not found"));
    }

    User createUser(CreateSubordinateRequest request, String roleName, User manager) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "Username already taken: " + request.username());
        }
        authService.validateStrongPassword(request.password());
        var role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Role not seeded: " + roleName));

        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName() != null ? request.fullName() : request.username());
        user.setManager(manager);
        user.getRoles().add(role);
        return userRepository.save(user);
    }

    private CourseResponse toResponse(Course course) {
        int subjectCount = subjectRepository.findByCourseRefId(course.getId()).size();
        return new CourseResponse(
                course.getId(),
                course.getCourseName(),
                course.getSemester(),
                course.getSyllabusYear(),
                course.getProgram() == null ? null : course.getProgram().getId(),
                course.getProgram() == null ? null : course.getProgram().getName(),
                course.getBo() == null ? null : course.getBo().getFullName(),
                course.getBo() == null ? null : course.getBo().getId(),
                course.getCreatedBy().getFullName(),
                subjectCount,
                course.getCreatedAt());
    }

    private SubjectProgressResponse toProgressResponse(Subject subject) {
        List<ValuationScript> scripts = scriptRepository.findBySubjectId(subject.getId());
        int total = scripts.size();
        int valuated = (int) scripts.stream()
                .filter(s -> s.getStatus() == ValuationStatus.VALUATED
                        || s.getStatus() == ValuationStatus.REVIEWED
                        || s.getStatus() == ValuationStatus.APPROVED)
                .count();
        int reviewed = (int) scripts.stream()
                .filter(s -> s.getStatus() == ValuationStatus.REVIEWED || s.getStatus() == ValuationStatus.APPROVED)
                .count();
        return new SubjectProgressResponse(
                subject.getId(), subject.getSubjectCode(), subject.getSubjectName(),
                subject.getCourse(), subject.getSemester(), subject.getQpCode(), subject.getStatus().name(),
                subject.getChief() == null ? null : subject.getChief().getFullName(),
                subject.getChief() == null ? null : subject.getChief().getId(),
                total, valuated, reviewed);
    }
}
