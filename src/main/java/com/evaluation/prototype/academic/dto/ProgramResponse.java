package com.evaluation.prototype.academic.dto;

import java.util.UUID;

public record ProgramResponse(
        UUID id,
        String name,
        String code,
        int durationSemesters,
        UUID departmentId,
        String departmentName,
        int batchCount) {
}
