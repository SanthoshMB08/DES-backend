package com.evaluation.prototype.valuation.dto;

import java.util.UUID;

public record SubordinateResponse(
        UUID id,
        String username,
        String fullName,
        String role,
        boolean available,
        UUID subjectId,
        String activeBundleCode) {
}
