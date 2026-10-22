package com.evaluation.prototype.valuation.dto;

import java.util.UUID;

public record ScannerRegistrationResponse(
        UUID id,
        String usn,
        String qpCode,
        String examYearMonth,
        String studentName,
        boolean bookletAssigned) {
}
