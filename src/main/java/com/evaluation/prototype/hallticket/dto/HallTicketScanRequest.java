package com.evaluation.prototype.hallticket.dto;

import java.util.List;

import com.evaluation.prototype.common.enums.StudentType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record HallTicketScanRequest(
        @NotBlank String registerNumber,
        @NotBlank String studentName,
        @NotNull StudentType studentType,
        @NotEmpty List<@Valid HallTicketExamRequest> exams) {
}
