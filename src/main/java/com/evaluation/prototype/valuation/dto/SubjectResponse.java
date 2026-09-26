package com.evaluation.prototype.valuation.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SubjectResponse(
        UUID id,
        String subjectCode,
        String subjectName,
        String course,
        String semester,
        String qpCode,
        String status,
        BigDecimal maxMarks,
        BigDecimal credits,
        BigDecimal cieWeightage,
        BigDecimal seeWeightage,
        BigDecimal maxCieMarks,
        BigDecimal minPassMarks,
        UUID courseId,
        String chiefName,
        UUID chiefId,
        String openedByName,
        Instant createdAt) {
}
