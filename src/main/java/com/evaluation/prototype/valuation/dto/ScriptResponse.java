package com.evaluation.prototype.valuation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.evaluation.prototype.academic.dto.MarkingSection;

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
        String reviewComments,
        List<MarkingSection> markingScheme) {
}
