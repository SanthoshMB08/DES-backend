package com.evaluation.prototype.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class SupabaseStorageServiceTest {

    @Test
    void createsSeparateSafePathsForOriginalAndCorrectedFiles() {
        UUID scriptId = UUID.fromString("1c3572db-9650-41a9-b54f-22b6ef358519");

        assertEquals("QP_101/qp-original/" + scriptId + ".pdf",
                SupabaseStorageService.objectPath("QP/101", "qp-original", scriptId));
        assertEquals("QP_101/qp-after-correction/" + scriptId + ".pdf",
                SupabaseStorageService.objectPath("QP/101", "qp-after-correction", scriptId));
    }
}
