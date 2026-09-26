package com.evaluation.prototype.hallticket.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BarcodeScanRequest(
        @NotBlank String registerNumber,
        // The specific HallTicketExam the invigilator confirmed (from BarcodeLookupResponse.matches) -
        // resolves semester/examDate/qpCode server-side, so the client never has to send them.
        @NotNull UUID hallTicketExamId,
        @NotBlank String answerBookletNumber) {
}
