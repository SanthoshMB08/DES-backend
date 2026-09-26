package com.evaluation.prototype.valuation.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public record OpenCourseRequest(
        @NotBlank String courseName,
        @NotBlank String semester,
        @NotBlank String syllabusYear,
        // Optional link to an academic Program master record - see /api/admin/academic/programs.
        UUID programId,
        UUID syllabusId) {
}
