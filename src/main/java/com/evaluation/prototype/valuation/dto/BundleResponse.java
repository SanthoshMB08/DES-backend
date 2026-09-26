package com.evaluation.prototype.valuation.dto;

import java.time.Instant;
import java.util.UUID;

public record BundleResponse(
        UUID id,
        String bundleCode,
        String course,
        String semester,
        String qpCode,
        String subject,
        String centerCode,
        int totalScripts,
        String status,
        String valuatorName,
        String reviewerName,
        String boeName,
        Instant createdAt) {
}
