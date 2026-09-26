package com.evaluation.prototype.cie.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CieComponentResponse(
        UUID id,
        String name,
        BigDecimal maxMarks,
        int sequence) {
}
