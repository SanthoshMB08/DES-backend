package com.evaluation.prototype.hallticket.dto;

import java.time.LocalDate;

public record MobileExamEntry(
        LocalDate examDate,
        String qpCode,
        String status,               // PENDING | PRESENT | ABSENT
        String answerBookletNumber) {
}
