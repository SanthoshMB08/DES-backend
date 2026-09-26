package com.evaluation.prototype.academic.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AcademicSubjectResponse(UUID id, UUID syllabusId, String semester, String subjectCode,
        String subjectName, String qpCode, BigDecimal maxMarks, BigDecimal credits) {}
