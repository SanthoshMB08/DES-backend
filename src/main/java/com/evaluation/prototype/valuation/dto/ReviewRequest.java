package com.evaluation.prototype.valuation.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;

public record ReviewRequest(
        boolean approved,
        String comments,
        String marksJson,
        @DecimalMin(value = "0", message = "Total marks cannot be negative") BigDecimal totalMarks) {
}
