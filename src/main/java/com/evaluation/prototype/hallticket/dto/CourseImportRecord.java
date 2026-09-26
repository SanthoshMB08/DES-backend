package com.evaluation.prototype.hallticket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

// Field names/JSON keys match apps/worker/extract.py's Course dataclass exactly.
public record CourseImportRecord(
        @JsonProperty("sl_no") Integer slNo,
        String term,
        @JsonProperty("qp_code") @NotBlank String qpCode,
        @JsonProperty("course_code") String courseCode,
        @JsonProperty("course_name") String courseName,
        // exam_date arrives as "YYYY-MM-DD" from the OCR module - kept as a raw
        // String here (not LocalDate) so a malformed/unparseable row can be
        // skipped with a warning instead of failing the whole batch.
        @JsonProperty("exam_date") @NotBlank String examDate,
        @JsonProperty("exam_time") String examTime,
        @JsonProperty("centre_code") String centreCode) {
}
