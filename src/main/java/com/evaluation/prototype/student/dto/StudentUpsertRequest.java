package com.evaluation.prototype.student.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

// Used to both pre-load a student before any exam touches them, and to fill in
// details (batch, DOB, category, gender) on a student who already exists because
// an invigilator's scan created a bare-bones record first.
public record StudentUpsertRequest(
        @NotBlank String registerNumber,
        String studentName,
        UUID batchId,
        LocalDate dateOfBirth,
        String category,
        String gender) {
}
