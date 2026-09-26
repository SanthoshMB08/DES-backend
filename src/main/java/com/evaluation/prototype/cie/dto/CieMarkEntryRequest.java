package com.evaluation.prototype.cie.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CieMarkEntryRequest(
        @NotBlank String registerNumber,
        String studentName,
        @NotNull @DecimalMin(value = "0", message = "Marks cannot be negative") BigDecimal marksObtained) {
}
