package com.evaluation.prototype.cie.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CieComponentRequest(
        @NotBlank String name,
        @NotNull @DecimalMin(value = "1", message = "Max marks must be at least 1") BigDecimal maxMarks,
        Integer sequence) {
}
