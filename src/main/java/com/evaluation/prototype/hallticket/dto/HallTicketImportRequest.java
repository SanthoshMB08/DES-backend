package com.evaluation.prototype.hallticket.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

/**
 * Matches the OCR module's own job-status JSON shape ({"students": [...]})
 * almost exactly, so its extracted data can be forwarded here with minimal
 * translation - see apps/worker/extract.py's StudentRecord.to_dict() in the
 * ocr-module repo.
 */
public record HallTicketImportRequest(
        @NotEmpty @Valid List<StudentImportRecord> students) {
}
