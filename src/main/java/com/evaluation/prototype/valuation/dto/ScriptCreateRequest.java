package com.evaluation.prototype.valuation.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;

public record ScriptCreateRequest(
        @NotBlank String answerBookletId,
        String candidateId,
        String candidateName,
        @NotBlank String qpCode,
        String subject,
        LocalDate examDate,
        String course,
        String semester,
        String centerCode,
        String scannedFileName) {
}
