package com.evaluation.prototype.valuation.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

// Course + semester are inherited from the parent Course (see /api/re/courses/{courseId}/subjects) -
// the RE only needs to supply what's specific to this one QP code. All the CIE/SEE
// metadata fields are optional - a college not ready to configure that yet can leave
// them blank and add it later once Phase 3 (internal marks) is in use.
public record CreateSubjectRequest(
        @NotBlank String subjectCode,
        String subjectName,
        @NotBlank String qpCode,
        // Defaults to 100 (the common case) if not supplied.
        @DecimalMin(value = "1", message = "Max marks must be at least 1") BigDecimal maxMarks,
        BigDecimal credits,
        @DecimalMin(value = "0", message = "CIE weightage cannot be negative") BigDecimal cieWeightage,
        @DecimalMin(value = "0", message = "SEE weightage cannot be negative") BigDecimal seeWeightage,
        @DecimalMin(value = "1", message = "Max CIE marks must be at least 1") BigDecimal maxCieMarks,
        @DecimalMin(value = "0", message = "Min pass marks cannot be negative") BigDecimal minPassMarks) {
}
