package com.evaluation.prototype.attendance.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookletScanRequest(
        @NotBlank String registerNumber,
        @NotNull LocalDate examDate,
        @NotBlank String qpCode,
        @NotBlank String answerBookletNumber) {
}
