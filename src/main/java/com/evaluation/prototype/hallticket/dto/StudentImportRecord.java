package com.evaluation.prototype.hallticket.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

// Field names/JSON keys match apps/worker/extract.py's StudentRecord exactly
// (snake_case, since that's what asdict() on the Python dataclass produces).
public record StudentImportRecord(
        @JsonProperty("academic_year") String academicYear,
        @JsonProperty("month_of_exam") String monthOfExam,
        @JsonProperty("college_name") String collegeName,
        @JsonProperty("college_code") String collegeCode,
        @JsonProperty("program_level") String programLevel,
        @JsonProperty("program_name") String programName,
        String discipline,
        @NotBlank String semester,
        @NotBlank String uucms,
        @JsonProperty("student_name") String studentName,
        @NotEmpty @Valid List<CourseImportRecord> courses) {
}
