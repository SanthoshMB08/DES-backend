package com.evaluation.prototype.valuation.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record ScriptSubmitRequest(
        String marksJson,
        @NotNull @DecimalMin(value = "0", message = "Total marks cannot be negative") BigDecimal totalMarks) {
}
