package com.evaluation.prototype.valuation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ScriptResponse(
        UUID id,
        UUID bundleId,
        String bundleCode,
        String answerBookletId,
        String candidateId,
        String candidateName,
        String qpCode,
        String subject,
        LocalDate examDate,
        String course,
        String semester,
        String centerCode,
        String scannedFileName,
        String fileUrl,
        String status,
        String marksJson,
        BigDecimal totalMarks,
        String reviewComments) {
}
