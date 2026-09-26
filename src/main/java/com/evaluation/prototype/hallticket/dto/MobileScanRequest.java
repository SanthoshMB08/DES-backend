package com.evaluation.prototype.hallticket.dto;

import java.time.LocalDate;

import com.evaluation.prototype.common.enums.StudentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * One QR/booklet scan on the mobile invigilator screen = one exam attendance
 * marked. studentName/studentType are only required the first time a given
 * (registerNumber, semester) pair is seen - the frontend should call
 * /api/invigilator/mobile/lookup first to know whether to show those fields.
 */
public record MobileScanRequest(
        @NotBlank String registerNumber,
        String studentName,
        StudentType studentType,
        @NotBlank String semester,
        LocalDate examDate,
        @NotBlank String qpCode,
        @NotBlank String answerBookletNumber) {
}
