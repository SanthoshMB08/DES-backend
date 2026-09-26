package com.evaluation.prototype.academic.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
import com.evaluation.prototype.academic.dto.AcademicCurriculumRequest;
import com.evaluation.prototype.academic.dto.AcademicCurriculumResponse;
import com.evaluation.prototype.academic.service.AcademicStructureService;

import jakarta.validation.Valid;

/**
 * Academic master-data setup: Department -&gt; Program -&gt; Batch. This is
 * Admin-managed, not RE-managed - it's set up once per college (or occasionally,
 * when a new program launches), unlike Course/Subject which the RE opens every
 * evaluation cycle.
 */
@RestController
@RequestMapping("/api/admin/academic")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAcademicController {

    private final AcademicStructureService academicStructureService;

    public AdminAcademicController(AcademicStructureService academicStructureService) {
        this.academicStructureService = academicStructureService;
    }

    @PostMapping("/departments")
    public DepartmentResponse createDepartment(@Valid @RequestBody DepartmentRequest request) {
        return academicStructureService.createDepartment(request);
    }

    @PutMapping("/departments/{departmentId}")
    public DepartmentResponse updateDepartment(@PathVariable UUID departmentId,
            @Valid @RequestBody DepartmentRequest request) {
        return academicStructureService.updateDepartment(departmentId, request);
    }

    @GetMapping("/departments")
    public List<DepartmentResponse> departments() {
        return academicStructureService.listDepartments();
    }

    @PostMapping("/programs")
    public ProgramResponse createProgram(@Valid @RequestBody ProgramRequest request) {
        return academicStructureService.createProgram(request);
    }

    @PutMapping("/programs/{programId}")
    public ProgramResponse updateProgram(@PathVariable UUID programId, @Valid @RequestBody ProgramRequest request) {
        return academicStructureService.updateProgram(programId, request);
    }

    @GetMapping("/programs")
    public List<ProgramResponse> programs() {
        return academicStructureService.listPrograms();
    }

    @PostMapping("/programs/{programId}/batches")
    public BatchResponse createBatch(@PathVariable UUID programId, @Valid @RequestBody BatchRequest request) {
        return academicStructureService.createBatch(programId, request);
    }

    @GetMapping("/batches")
    public List<BatchResponse> batches() {
        return academicStructureService.listBatches();
    }

    @GetMapping("/programs/{programId}/batches")
    public List<BatchResponse> batchesForProgram(@PathVariable UUID programId) {
        return academicStructureService.listBatchesForProgram(programId);
    }

    @PostMapping("/programs/{programId}/syllabi")
    public SyllabusResponse createSyllabus(@PathVariable UUID programId, @Valid @RequestBody SyllabusRequest request) {
        return academicStructureService.createSyllabus(programId, request);
    }

    @GetMapping("/syllabi")
    public List<SyllabusResponse> syllabi() {
        return academicStructureService.listSyllabi();
    }

    @PostMapping("/syllabi")
    public SyllabusResponse createSyllabus(@Valid @RequestBody SyllabusRequest request) {
        return academicStructureService.createSyllabus(request);
    }

    @PutMapping("/syllabi/{syllabusId}")
    public SyllabusResponse updateSyllabus(@PathVariable UUID syllabusId,
            @Valid @RequestBody SyllabusRequest request) {
        return academicStructureService.updateSyllabus(syllabusId, request);
    }

    @GetMapping("/programs/{programId}/syllabi")
    public List<SyllabusResponse> syllabiForProgram(@PathVariable UUID programId) {
        return academicStructureService.listSyllabiForProgram(programId);
    }

    @PostMapping("/syllabi/{syllabusId}/subjects")
    public AcademicSubjectResponse createAcademicSubject(@PathVariable UUID syllabusId,
            @Valid @RequestBody AcademicSubjectRequest request) {
        return academicStructureService.createAcademicSubject(syllabusId, request);
    }

    @GetMapping("/syllabi/{syllabusId}/subjects")
    public List<AcademicSubjectResponse> academicSubjects(@PathVariable UUID syllabusId,
            @org.springframework.web.bind.annotation.RequestParam String semester) {
        return academicStructureService.listAcademicSubjects(syllabusId, semester);
    }

    @PostMapping("/curricula")
    public AcademicCurriculumResponse createCurriculum(@Valid @RequestBody AcademicCurriculumRequest request) {
        return academicStructureService.createCurriculum(request);
    }

    @PutMapping("/curricula/{curriculumId}")
    public AcademicCurriculumResponse updateCurriculum(@PathVariable UUID curriculumId,
            @Valid @RequestBody AcademicCurriculumRequest request) {
        return academicStructureService.updateCurriculum(curriculumId, request);
    }

    @GetMapping("/curricula")
    public List<AcademicCurriculumResponse> curricula() {
        return academicStructureService.listCurricula();
    }

    @PostMapping("/curricula/{curriculumId}/subjects")
    public AcademicSubjectResponse createCurriculumSubject(@PathVariable UUID curriculumId,
            @Valid @RequestBody AcademicSubjectRequest request) {
        return academicStructureService.createAcademicSubjectForCurriculum(curriculumId, request);
    }

    @PutMapping("/curricula/{curriculumId}/subjects/{subjectId}")
    public AcademicSubjectResponse updateCurriculumSubject(@PathVariable UUID curriculumId,
            @PathVariable UUID subjectId, @Valid @RequestBody AcademicSubjectRequest request) {
        return academicStructureService.updateAcademicSubjectForCurriculum(curriculumId, subjectId, request);
    }

    @GetMapping("/curricula/{curriculumId}/subjects")
    public List<AcademicSubjectResponse> curriculumSubjects(@PathVariable UUID curriculumId) {
        return academicStructureService.listAcademicSubjectsForCurriculum(curriculumId);
    }
}
