package com.evaluation.prototype.academic.dto;

import java.math.BigDecimal;

public record MarkingSection(
        String name,
        BigDecimal maxMarks,
        BigDecimal marksPerQuestion,
        int questionCount) {
}
