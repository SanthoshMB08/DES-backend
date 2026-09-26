package com.evaluation.prototype.academic.dto;

import java.util.UUID;

public record DepartmentResponse(
        UUID id,
        String name,
        String code,
        int programCount) {
}
