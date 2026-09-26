package com.evaluation.prototype.hallticket.dto;

import java.time.LocalDate;
import java.util.UUID;

// One exam scheduled for a student today - resolved automatically from their
// hall ticket data, no manual date/QP-code entry needed.
public record BarcodeExamMatch(
        UUID hallTicketExamId,
        String semester,
        LocalDate examDate,
        String qpCode,
        String courseName,
        String examTime,
        String centreCode,
        String attendanceStatus, // PENDING | PRESENT | ABSENT
        String answerBookletNumber) {
}
