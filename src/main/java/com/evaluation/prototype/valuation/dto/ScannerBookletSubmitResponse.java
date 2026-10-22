package com.evaluation.prototype.valuation.dto;

import java.util.UUID;

public record ScannerBookletSubmitResponse(
        UUID scriptId,
        UUID bookletRecordId,
        String bookletNumber,
        String originalPdfPath) {
}
