package com.evaluation.prototype.academic.dto;

import jakarta.validation.constraints.NotBlank;

public record BatchRequest(
        @NotBlank String admissionYear,
        String label) {
}
