package com.evaluation.prototype.academic.dto;

import java.util.UUID;

public record BatchResponse(
        UUID id,
        String admissionYear,
        String label,
        UUID programId,
        String programName,
        int studentCount) {
}
