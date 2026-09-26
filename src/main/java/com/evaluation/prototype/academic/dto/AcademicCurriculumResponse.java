package com.evaluation.prototype.academic.dto;

import java.util.UUID;

public record AcademicCurriculumResponse(UUID id, UUID programId, String programName, UUID syllabusId,
        String syllabusName, String syllabusYear, String semester, long subjectCount) {}
