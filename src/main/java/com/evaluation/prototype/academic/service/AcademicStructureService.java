package com.evaluation.prototype.academic.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.evaluation.prototype.academic.dto.BatchRequest;
import com.evaluation.prototype.academic.dto.BatchResponse;
import com.evaluation.prototype.academic.dto.DepartmentRequest;
import com.evaluation.prototype.academic.dto.DepartmentResponse;
import com.evaluation.prototype.academic.dto.ProgramRequest;
import com.evaluation.prototype.academic.dto.ProgramResponse;
import com.evaluation.prototype.academic.dto.SyllabusRequest;
import com.evaluation.prototype.academic.dto.SyllabusResponse;
import com.evaluation.prototype.academic.dto.AcademicSubjectRequest;
import com.evaluation.prototype.academic.dto.AcademicSubjectResponse;
import com.evaluation.prototype.academic.dto.MarkingSection;
import com.evaluation.prototype.academic.entity.Batch;
import com.evaluation.prototype.academic.entity.Department;
import com.evaluation.prototype.academic.entity.Program;
import com.evaluation.prototype.academic.repository.BatchRepository;
import com.evaluation.prototype.academic.repository.DepartmentRepository;
import com.evaluation.prototype.academic.repository.ProgramRepository;
import com.evaluation.prototype.academic.repository.SyllabusRepository;
import com.evaluation.prototype.academic.repository.AcademicSubjectRepository;
import com.evaluation.prototype.academic.entity.Syllabus;
import com.evaluation.prototype.academic.entity.AcademicSubject;
import com.evaluation.prototype.academic.entity.AcademicCurriculum;
import com.evaluation.prototype.academic.dto.AcademicCurriculumRequest;
import com.evaluation.prototype.academic.dto.AcademicCurriculumResponse;
import com.evaluation.prototype.academic.repository.AcademicCurriculumRepository;
import com.evaluation.prototype.auth.service.CurrentUserService;
import com.evaluation.prototype.common.exception.ApiException;
import com.evaluation.prototype.student.repository.StudentRepository;
import com.evaluation.prototype.user.entity.User;

/**
 * Academic master data: Department -&gt; Program -&gt; Batch. Set up once (or
 * occasionally) by an Admin, then referenced everywhere else - Courses link to a
 * Program, Students link to a Batch.
 */
@Service
public class AcademicStructureService {

    private static final Logger log = LoggerFactory.getLogger(AcademicStructureService.class);

    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final BatchRepository batchRepository;
    private final StudentRepository studentRepository;
    private final CurrentUserService currentUserService;
    private final SyllabusRepository syllabusRepository;
    private final AcademicSubjectRepository academicSubjectRepository;
    private final AcademicCurriculumRepository academicCurriculumRepository;

    public AcademicStructureService(
            DepartmentRepository departmentRepository,
            ProgramRepository programRepository,
            BatchRepository batchRepository,
            StudentRepository studentRepository,
            CurrentUserService currentUserService, SyllabusRepository syllabusRepository,
            AcademicSubjectRepository academicSubjectRepository,
            AcademicCurriculumRepository academicCurriculumRepository) {
        this.departmentRepository = departmentRepository;
        this.programRepository = programRepository;
        this.batchRepository = batchRepository;
        this.studentRepository = studentRepository;
        this.currentUserService = currentUserService;
        this.syllabusRepository = syllabusRepository;
        this.academicSubjectRepository = academicSubjectRepository;
        this.academicCurriculumRepository = academicCurriculumRepository;
    }

    private User currentUser() {
        return currentUserService.getCurrentUser()
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }

    // ---------- Department ----------

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        if (departmentRepository.findByCodeIgnoreCase(request.code()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "A department with this code already exists");
        }
        Department department = new Department();
        department.setName(request.name());
        department.setCode(request.code());
        department.setCreatedBy(currentUser());
        department = departmentRepository.save(department);
        return toResponse(department);
    }

    public List<DepartmentResponse> listDepartments() {
        return departmentRepository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public DepartmentResponse updateDepartment(UUID departmentId, DepartmentRequest request) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Department not found"));
        departmentRepository.findByCodeIgnoreCase(request.code()).ifPresent(existing -> {
            if (!existing.getId().equals(departmentId)) {
                throw new ApiException(HttpStatus.CONFLICT, "A department with this code already exists");
            }
        });
        department.setName(request.name());
        department.setCode(request.code());
        return toResponse(departmentRepository.save(department));
    }

    // ---------- Program ----------

    @Transactional
    public ProgramResponse createProgram(ProgramRequest request) {
        if (programRepository.findByCodeIgnoreCase(request.code()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "A program with this code already exists");
        }
        Program program = new Program();
        program.setDepartment(departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Department not found")));
        program.setName(request.name());
        program.setCode(request.code());
        program.setDurationSemesters(request.durationSemesters() != null ? request.durationSemesters() : 6);
        program.setCreatedBy(currentUser());
        program = programRepository.save(program);
        return toResponse(program);
    }

    public List<ProgramResponse> listPrograms() {
        return programRepository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ProgramResponse updateProgram(UUID programId, ProgramRequest request) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Program not found"));
        programRepository.findByCodeIgnoreCase(request.code()).ifPresent(existing -> {
            if (!existing.getId().equals(programId)) {
                throw new ApiException(HttpStatus.CONFLICT, "A program with this code already exists");
            }
        });
        program.setDepartment(departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Department not found")));
        program.setName(request.name());
        program.setCode(request.code());
        program.setDurationSemesters(request.durationSemesters() != null ? request.durationSemesters() : 6);
        return toResponse(programRepository.save(program));
    }

    // ---------- Batch ----------

    @Transactional
    public BatchResponse createBatch(UUID programId, BatchRequest request) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Program not found"));
        if (batchRepository.findByProgramIdAndAdmissionYear(programId, request.admissionYear()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "A batch for this program and admission year already exists");
        }
        Batch batch = new Batch();
        batch.setProgram(program);
        batch.setAdmissionYear(request.admissionYear());
        batch.setLabel(request.label() != null && !request.label().isBlank()
                ? request.label()
                : program.getName() + " " + request.admissionYear());
        batch.setCreatedBy(currentUser());
        batch = batchRepository.save(batch);
        return toResponse(batch);
    }

    public List<BatchResponse> listBatches() {
        return batchRepository.findAllByOrderByAdmissionYearDesc().stream().map(this::toResponse).toList();
    }

    public List<BatchResponse> listBatchesForProgram(UUID programId) {
        return batchRepository.findByProgramIdOrderByAdmissionYearDesc(programId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public SyllabusResponse createSyllabus(SyllabusRequest request) {
        if (syllabusRepository.findAllByOrderByYearDesc().stream()
                .anyMatch(s -> s.getProgram() == null && s.getName().equalsIgnoreCase(request.name())
                        && s.getYear().equalsIgnoreCase(request.year()))) {
            throw new ApiException(HttpStatus.CONFLICT, "This syllabus already exists");
        }
        Syllabus syllabus = new Syllabus();
        syllabus.setName(request.name());
        syllabus.setYear(request.year());
        syllabus.setCreatedBy(currentUser());
        return toResponse(syllabusRepository.save(syllabus));
    }

    @Transactional
    public SyllabusResponse createSyllabus(UUID programId, SyllabusRequest request) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Program not found"));
        if (syllabusRepository.findByProgramIdOrderByYearDesc(programId).stream()
                .anyMatch(s -> s.getName().equalsIgnoreCase(request.name()) && s.getYear().equalsIgnoreCase(request.year()))) {
            throw new ApiException(HttpStatus.CONFLICT, "This syllabus already exists for the program");
        }
        Syllabus syllabus = new Syllabus();
        syllabus.setProgram(program); syllabus.setName(request.name()); syllabus.setYear(request.year());
        syllabus.setCreatedBy(currentUser());
        return toResponse(syllabusRepository.save(syllabus));
    }

    public List<SyllabusResponse> listSyllabi() {
        return syllabusRepository.findAllByOrderByYearDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public SyllabusResponse updateSyllabus(UUID syllabusId, SyllabusRequest request) {
        Syllabus syllabus = syllabusRepository.findById(syllabusId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Syllabus not found"));
        boolean duplicate = syllabusRepository.findAllByOrderByYearDesc().stream()
                .anyMatch(existing -> !existing.getId().equals(syllabusId)
                        && java.util.Objects.equals(
                                existing.getProgram() == null ? null : existing.getProgram().getId(),
                                syllabus.getProgram() == null ? null : syllabus.getProgram().getId())
                        && existing.getName().equalsIgnoreCase(request.name())
                        && existing.getYear().equalsIgnoreCase(request.year()));
        if (duplicate) {
            throw new ApiException(HttpStatus.CONFLICT, "This syllabus already exists");
        }
        syllabus.setName(request.name());
        syllabus.setYear(request.year());
        return toResponse(syllabusRepository.save(syllabus));
    }

    public List<SyllabusResponse> listSyllabiForProgram(UUID programId) {
        return syllabusRepository.findByProgramIdIsNullOrProgramIdOrderByYearDesc(programId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AcademicCurriculumResponse createCurriculum(AcademicCurriculumRequest request) {
        Program program = programRepository.findById(request.programId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Program not found"));
        Syllabus syllabus = syllabusRepository.findById(request.syllabusId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Syllabus not found"));
        if (syllabus.getProgram() != null && !syllabus.getProgram().getId().equals(program.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Selected syllabus belongs to a different program");
        }
        if (academicCurriculumRepository.existsByProgramIdAndSyllabusIdAndSemester(
                request.programId(), request.syllabusId(), request.semester())) {
            throw new ApiException(HttpStatus.CONFLICT, "This program, semester, and syllabus entry already exists");
        }
        AcademicCurriculum curriculum = new AcademicCurriculum();
        curriculum.setProgram(program);
        curriculum.setSyllabus(syllabus);
        curriculum.setSemester(request.semester());
        curriculum.setCreatedBy(currentUser());
        return toResponse(academicCurriculumRepository.save(curriculum));
    }

    public List<AcademicCurriculumResponse> listCurricula() {
        return academicCurriculumRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public AcademicCurriculumResponse updateCurriculum(UUID curriculumId, AcademicCurriculumRequest request) {
        AcademicCurriculum curriculum = academicCurriculumRepository.findById(curriculumId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Curriculum entry not found"));
        Program program = programRepository.findById(request.programId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Program not found"));
        Syllabus syllabus = syllabusRepository.findById(request.syllabusId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Syllabus not found"));
        if (syllabus.getProgram() != null && !syllabus.getProgram().getId().equals(program.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Selected syllabus belongs to a different program");
        }
        boolean unchanged = curriculum.getProgram().getId().equals(program.getId())
                && curriculum.getSyllabus().getId().equals(syllabus.getId())
                && curriculum.getSemester().equals(request.semester());
        if (!unchanged && academicCurriculumRepository.existsByProgramIdAndSyllabusIdAndSemester(
                program.getId(), syllabus.getId(), request.semester())) {
            throw new ApiException(HttpStatus.CONFLICT, "This program, semester, and syllabus entry already exists");
        }
        List<AcademicSubject> subjects = academicSubjectRepository
                .findByCurriculumIdOrderBySubjectCodeAsc(curriculumId);
        List<AcademicSubject> targetSubjects = academicSubjectRepository
                .findBySyllabusIdAndSemester(syllabus.getId(), request.semester());
        boolean subjectCodeConflict = targetSubjects.stream()
                .filter(existing -> subjects.stream().noneMatch(own -> own.getId().equals(existing.getId())))
                .anyMatch(existing -> subjects.stream()
                        .anyMatch(own -> own.getQpCode().equalsIgnoreCase(existing.getQpCode())));
        if (subjectCodeConflict) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Changing this curriculum would duplicate a subject QP code in the selected syllabus and semester");
        }
        curriculum.setProgram(program);
        curriculum.setSyllabus(syllabus);
        curriculum.setSemester(request.semester());
        curriculum = academicCurriculumRepository.save(curriculum);
        subjects.forEach(subject -> {
            subject.setSyllabus(syllabus);
            subject.setSemester(request.semester());
        });
        academicSubjectRepository.saveAll(subjects);
        return toResponse(curriculum);
    }

    private AcademicCurriculumResponse toResponse(AcademicCurriculum value) {
        return new AcademicCurriculumResponse(value.getId(), value.getProgram().getId(), value.getProgram().getName(),
                value.getSyllabus().getId(), value.getSyllabus().getName(), value.getSyllabus().getYear(),
                value.getSemester(), academicSubjectRepository.countByCurriculumId(value.getId()));
    }

    @Transactional
    public AcademicSubjectResponse createAcademicSubject(UUID syllabusId, AcademicSubjectRequest request) {
        Syllabus syllabus = syllabusRepository.findById(syllabusId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Syllabus not found"));
        AcademicSubject subject = new AcademicSubject();
        subject.setSyllabus(syllabus); subject.setSemester(request.semester());
        subject.setSubjectCode(request.subjectCode()); subject.setSubjectName(request.subjectName());
        subject.setQpCode(request.qpCode());
        subject.setMaxMarks(request.maxMarks() == null ? java.math.BigDecimal.valueOf(100) : request.maxMarks());
        subject.setMarkingScheme(validateMarkingScheme(request.markingScheme(), subject.getMaxMarks()));
        subject.setCredits(request.credits()); subject.setCreatedBy(currentUser());
        subject = academicSubjectRepository.save(subject);
        log.info("Created academic subject {} with {} configured marking sections",
                subject.getId(), subject.getMarkingScheme().size());
        return toResponse(subject);
    }

    @Transactional
    public AcademicSubjectResponse createAcademicSubjectForCurriculum(UUID curriculumId, AcademicSubjectRequest request) {
        AcademicCurriculum curriculum = academicCurriculumRepository.findById(curriculumId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Curriculum entry not found"));
        ensureUniqueQpCode(curriculum.getSyllabus().getId(), curriculum.getSemester(), request.qpCode(), null);
        AcademicSubject subject = new AcademicSubject();
        subject.setCurriculum(curriculum);
        subject.setSyllabus(curriculum.getSyllabus());
        subject.setSemester(curriculum.getSemester());
        subject.setSubjectCode(request.subjectCode());
        subject.setSubjectName(request.subjectName());
        subject.setQpCode(request.qpCode());
        subject.setMaxMarks(request.maxMarks() == null ? java.math.BigDecimal.valueOf(100) : request.maxMarks());
        subject.setMarkingScheme(validateMarkingScheme(request.markingScheme(), subject.getMaxMarks()));
        subject.setCredits(request.credits());
        subject.setCreatedBy(currentUser());
        subject = academicSubjectRepository.save(subject);
        log.info("Created academic subject {} with {} configured marking sections",
                subject.getId(), subject.getMarkingScheme().size());
        return toResponse(subject);
    }

    @Transactional
    public AcademicSubjectResponse updateAcademicSubjectForCurriculum(UUID curriculumId, UUID subjectId,
            AcademicSubjectRequest request) {
        AcademicCurriculum curriculum = academicCurriculumRepository.findById(curriculumId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Curriculum entry not found"));
        AcademicSubject subject = academicSubjectRepository.findById(subjectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Academic subject not found"));
        if (subject.getCurriculum() == null || !subject.getCurriculum().getId().equals(curriculumId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Subject does not belong to this curriculum");
        }
        ensureUniqueQpCode(curriculum.getSyllabus().getId(), curriculum.getSemester(), request.qpCode(), subjectId);
        subject.setSyllabus(curriculum.getSyllabus());
        subject.setSemester(curriculum.getSemester());
        subject.setSubjectCode(request.subjectCode());
        subject.setSubjectName(request.subjectName());
        subject.setQpCode(request.qpCode());
        subject.setMaxMarks(request.maxMarks() == null
                ? java.math.BigDecimal.valueOf(100) : request.maxMarks());
        subject.setMarkingScheme(validateMarkingScheme(request.markingScheme(), subject.getMaxMarks()));
        subject.setCredits(request.credits());
        subject = academicSubjectRepository.save(subject);
        log.info("Updated academic subject {} with {} configured marking sections",
                subject.getId(), subject.getMarkingScheme().size());
        return toResponse(subject);
    }

    public List<AcademicSubjectResponse> listAcademicSubjectsForCurriculum(UUID curriculumId) {
        return academicSubjectRepository.findByCurriculumIdOrderBySubjectCodeAsc(curriculumId)
                .stream().map(this::toResponse).toList();
    }

    public List<AcademicSubjectResponse> listAcademicSubjects(UUID syllabusId, String semester) {
        return academicSubjectRepository.findBySyllabusIdAndSemesterOrderBySubjectCodeAsc(syllabusId, semester)
                .stream().map(this::toResponse).toList();
    }

    private void ensureUniqueQpCode(UUID syllabusId, String semester, String qpCode, UUID excludedSubjectId) {
        boolean duplicate = academicSubjectRepository.findBySyllabusIdAndSemester(syllabusId, semester).stream()
                .anyMatch(subject -> !subject.getId().equals(excludedSubjectId)
                        && subject.getQpCode().equalsIgnoreCase(qpCode));
        if (duplicate) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "A subject with this QP code already exists in the curriculum");
        }
    }

    private List<MarkingSection> validateMarkingScheme(List<MarkingSection> sections, java.math.BigDecimal subjectMaxMarks) {
        if (sections == null || sections.isEmpty()) {
            return List.of();
        }
        java.util.Set<String> names = new java.util.HashSet<>();
        java.math.BigDecimal configuredTotal = java.math.BigDecimal.ZERO;
        for (MarkingSection section : sections) {
            if (section == null || section.name() == null || section.name().isBlank()
                    || section.maxMarks() == null || section.maxMarks().signum() <= 0
                    || section.marksPerQuestion() == null || section.marksPerQuestion().signum() <= 0
                    || section.questionCount() <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Each marking section needs a name, positive maximum marks, positive marks per question, and at least one question");
            }
            if (!names.add(section.name().trim().toLowerCase(java.util.Locale.ROOT))) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Marking section names must be unique");
            }
            if (section.marksPerQuestion().multiply(java.math.BigDecimal.valueOf(section.questionCount()))
                    .compareTo(section.maxMarks()) < 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "The section maximum cannot exceed the total marks available from its questions");
            }
            configuredTotal = configuredTotal.add(section.maxMarks());
        }
        if (subjectMaxMarks == null || configuredTotal.compareTo(subjectMaxMarks) != 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "The sum of section maximum marks must equal the subject maximum marks");
        }
        return List.copyOf(sections);
    }

    // ---------- mapping ----------

    private DepartmentResponse toResponse(Department department) {
        int programCount = programRepository.findByDepartmentIdOrderByNameAsc(department.getId()).size();
        return new DepartmentResponse(department.getId(), department.getName(), department.getCode(), programCount);
    }

    private ProgramResponse toResponse(Program program) {
        int batchCount = batchRepository.findByProgramIdOrderByAdmissionYearDesc(program.getId()).size();
        return new ProgramResponse(
                program.getId(),
                program.getName(),
                program.getCode(),
                program.getDurationSemesters(),
                program.getDepartment() == null ? null : program.getDepartment().getId(),
                program.getDepartment() == null ? null : program.getDepartment().getName(),
                batchCount);
    }

    private BatchResponse toResponse(Batch batch) {
        long studentCount = studentRepository.countByBatchId(batch.getId());
        return new BatchResponse(
                batch.getId(),
                batch.getAdmissionYear(),
                batch.getLabel(),
                batch.getProgram().getId(),
                batch.getProgram().getName(),
                (int) studentCount);
    }

    private SyllabusResponse toResponse(Syllabus syllabus) {
        return new SyllabusResponse(syllabus.getId(),
                syllabus.getProgram() == null ? null : syllabus.getProgram().getId(),
                syllabus.getProgram() == null ? null : syllabus.getProgram().getName(),
                syllabus.getName(), syllabus.getYear());
    }

    private AcademicSubjectResponse toResponse(AcademicSubject subject) {
        return new AcademicSubjectResponse(subject.getId(), subject.getSyllabus().getId(), subject.getSemester(),
                subject.getSubjectCode(), subject.getSubjectName(), subject.getQpCode(),
                subject.getMaxMarks(), subject.getCredits(), subject.getMarkingScheme());
    }
}
