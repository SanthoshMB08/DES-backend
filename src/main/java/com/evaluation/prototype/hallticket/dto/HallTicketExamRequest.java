package com.evaluation.prototype.hallticket.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HallTicketExamRequest(
        @NotNull LocalDate examDate,
        @NotBlank String qpCode) {
}
