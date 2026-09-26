package com.evaluation.prototype.valuation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.evaluation.prototype.valuation.dto.CourseResponse;
import com.evaluation.prototype.valuation.dto.CreateSubjectRequest;
import com.evaluation.prototype.valuation.dto.CreateSubordinateRequest;
import com.evaluation.prototype.valuation.dto.OpenCourseRequest;
import com.evaluation.prototype.valuation.dto.SubjectProgressResponse;
import com.evaluation.prototype.valuation.dto.SubjectResponse;
import com.evaluation.prototype.valuation.dto.SubordinateResponse;
import com.evaluation.prototype.valuation.service.CourseService;
import com.evaluation.prototype.valuation.service.SubjectService;
import org.springframework.web.bind.annotation.PostMapping;
import com.evaluation.prototype.academic.dto.AcademicCurriculumResponse;
import com.evaluation.prototype.academic.dto.DepartmentResponse;
import com.evaluation.prototype.academic.dto.ProgramResponse;
import com.evaluation.prototype.academic.service.AcademicStructureService;

import jakarta.validation.Valid;

/**
 * RE (Register of Evaluation): creates Courses (course + semester), appoints one Board
 * Officer per Course, and opens Subjects (QP codes) within a Course. Also closes evaluation
 * and publishes marks for individual subjects once every paper is reviewed.
 */
@RestController
@RequestMapping("/api/re")
@PreAuthorize("hasAnyRole('ADMIN','RE')")
public class ReController {

    private final CourseService courseService;
    private final SubjectService subjectService;
    private final AcademicStructureService academicStructureService;

    public ReController(CourseService courseService, SubjectService subjectService,
            AcademicStructureService academicStructureService) {
        this.courseService = courseService;
        this.subjectService = subjectService;
        this.academicStructureService = academicStructureService;
    }

    @GetMapping("/academic/departments")
    public List<DepartmentResponse> academicDepartments() {
        return academicStructureService.listDepartments();
    }

    @GetMapping("/academic/programs")
    public List<ProgramResponse> academicPrograms() {
        return academicStructureService.listPrograms();
    }

    @GetMapping("/academic/curricula")
    public List<AcademicCurriculumResponse> academicCurricula() {
        return academicStructureService.listCurricula();
    }

    // ---------- Courses ----------

    @PostMapping("/courses")
    public CourseResponse createCourse(@Valid @RequestBody OpenCourseRequest request) {
        return courseService.createCourse(request);
    }

    @GetMapping("/courses")
    public List<CourseResponse> courses() {
        return courseService.listCourses();
    }

    @PostMapping("/courses/{courseId}/bo")
    public SubordinateResponse assignBo(@PathVariable UUID courseId, @Valid @RequestBody CreateSubordinateRequest request) {
        return courseService.assignBo(courseId, request);
    }

    // ---------- Subjects (QP codes) within a Course ----------

    @PostMapping("/courses/{courseId}/subjects")
    public SubjectResponse createSubject(@PathVariable UUID courseId, @Valid @RequestBody CreateSubjectRequest request) {
        return subjectService.createSubject(courseId, request);
    }

    @GetMapping("/courses/{courseId}/subjects")
    public List<SubjectResponse> subjectsForCourse(@PathVariable UUID courseId) {
        return subjectService.listSubjectsForCourse(courseId);
    }

    /** Flat view of every subject across every course - handy for a single overview table. */
    @GetMapping("/subjects")
    public List<SubjectResponse> subjects() {
        return subjectService.listSubjects();
    }

    /** Release results only after the BO has completed every subject in the course. */
    @PostMapping("/courses/{courseId}/release")
    public List<SubjectResponse> releaseCourse(@PathVariable UUID courseId) {
        return subjectService.releaseCourse(courseId);
    }

    /** Get valuation progress for a subject. */
    @GetMapping("/subjects/{subjectId}/progress")
    public SubjectProgressResponse getProgress(@PathVariable UUID subjectId) {
        return subjectService.getSubjectProgress(subjectId);
    }
}
