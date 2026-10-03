package com.evaluation.prototype.academic.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record AcademicSubjectRequest(
        @NotBlank String semester,
        @NotBlank String subjectCode,
        @NotBlank String subjectName,
        @NotBlank String qpCode,
        BigDecimal maxMarks,
        BigDecimal credits,
        List<MarkingSection> markingScheme) {}
