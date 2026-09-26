package com.evaluation.prototype.valuation.dto;

import java.time.Instant;
import java.util.UUID;

public record CourseResponse(
        UUID id,
        String courseName,
        String semester,
        String syllabusYear,
        UUID programId,
        String programName,
        String boName,
        UUID boId,
        String openedByName,
        int subjectCount,
        Instant createdAt) {
}
