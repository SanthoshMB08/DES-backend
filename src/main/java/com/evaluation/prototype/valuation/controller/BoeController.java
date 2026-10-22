package com.evaluation.prototype.valuation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.evaluation.prototype.valuation.dto.CourseResponse;
import com.evaluation.prototype.valuation.dto.BoeSubjectReportResponse;
import com.evaluation.prototype.valuation.dto.CreateSubordinateRequest;
import com.evaluation.prototype.valuation.dto.SubjectProgressResponse;
import com.evaluation.prototype.valuation.dto.SubjectResponse;
import com.evaluation.prototype.valuation.dto.SubordinateResponse;
import com.evaluation.prototype.valuation.service.CourseService;
import com.evaluation.prototype.valuation.service.BoeReportService;
import com.evaluation.prototype.valuation.service.SubjectService;

import jakarta.validation.Valid;

/**
 * BO (Board Officer): appointed by the RE to oversee one whole Course. Sees the status of
 * every paper for every subject in that course at a glance, and appoints a Subject Chief to
 * run each subject's evaluation (the Chief then appoints the Reviewer, forms bundles of 20,
 * and assigns them to Evaluators - see ChiefController).
 */
@RestController
@RequestMapping("/api/bo")
@PreAuthorize("hasAnyRole('ADMIN','BOE')")
public class BoeController {

    private final CourseService courseService;
    private final SubjectService subjectService;
    private final BoeReportService boeReportService;

    public BoeController(CourseService courseService, SubjectService subjectService, BoeReportService boeReportService) {
        this.courseService = courseService;
        this.subjectService = subjectService;
        this.boeReportService = boeReportService;
    }

    @GetMapping("/course")
    public CourseResponse myCourse() {
        return courseService.getMyCourse();
    }

    /** Status of every paper, grouped by subject, across the BO's whole course. */
    @GetMapping("/subjects")
    public List<SubjectProgressResponse> subjects() {
        return courseService.listSubjectsForMyCourse();
    }

    @GetMapping("/subjects/{subjectId}/report")
    public BoeSubjectReportResponse subjectReport(@PathVariable UUID subjectId) {
        return boeReportService.getSubjectReport(subjectId);
    }

    @GetMapping("/subjects/{subjectId}/report.xlsx")
    public ResponseEntity<byte[]> subjectReportExcel(@PathVariable UUID subjectId) {
        BoeSubjectReportResponse report = boeReportService.getSubjectReport(subjectId);
        byte[] workbook = boeReportService.exportSubjectExcel(subjectId);
        String filename = report.qpCode().replaceAll("[^A-Za-z0-9._-]", "_") + "-results.xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(workbook);
    }

    @PostMapping("/subjects/{subjectId}/chief")
    public SubordinateResponse assignChief(@PathVariable UUID subjectId, @Valid @RequestBody CreateSubordinateRequest request) {
        return courseService.assignChief(subjectId, request);
    }

    @PostMapping("/subjects/{subjectId}/complete")
    public SubjectResponse completeSubject(@PathVariable UUID subjectId) {
        return subjectService.completeSubjectForBo(subjectId);
    }
}
