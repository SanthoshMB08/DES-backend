package com.evaluation.prototype.valuation.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BoeSubjectReportResponse(
        UUID subjectId,
        String subjectCode,
        String subjectName,
        String qpCode,
        String course,
        String semester,
        int totalPapers,
        int markedPapers,
        List<ResultRow> results) {

    public record ResultRow(String roleNumber, BigDecimal marks, String status) {
    }
}
