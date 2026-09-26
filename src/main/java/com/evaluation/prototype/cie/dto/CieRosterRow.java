package com.evaluation.prototype.cie.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

// One row of the CIE marks sheet: a student, their marks per component (keyed by
// component id), and the running total.
public record CieRosterRow(
        UUID studentId,
        String registerNumber,
        String studentName,
        Map<UUID, BigDecimal> marksByComponent,
        BigDecimal total) {
}
