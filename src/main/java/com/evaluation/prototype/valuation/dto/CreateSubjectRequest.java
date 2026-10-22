package com.evaluation.prototype.valuation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

public record CreateSubjectRequest(
        @NotBlank String subjectCode,
        String subjectName,
        @NotBlank String qpCode,
        @DecimalMin(value = "1", message = "Max marks must be at least 1") java.math.BigDecimal maxMarks,
        java.math.BigDecimal credits) {
}
