package com.evaluation.prototype.hod.dto;

public record ExamRegistrationImportResponse(
        int rowsProcessed,
        int registrationsCreated,
        int registrationsAlreadyPresent) {
}
