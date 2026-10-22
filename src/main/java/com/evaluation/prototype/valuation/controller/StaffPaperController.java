package com.evaluation.prototype.valuation.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.evaluation.prototype.valuation.service.ValuationService;

@RestController
@RequestMapping("/api/staff/papers")
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class StaffPaperController {

    private final ValuationService valuationService;

    public StaffPaperController(ValuationService valuationService) {
        this.valuationService = valuationService;
    }

    @GetMapping("/corrected")
    public ResponseEntity<byte[]> downloadCorrected(
            @RequestParam String roleNumber,
            @RequestParam String qpCode) {
        byte[] bytes = valuationService.getCorrectedScriptFile(roleNumber, qpCode);
        String filename = safeFilenamePart(roleNumber) + "-" + safeFilenamePart(qpCode) + "-corrected.pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(bytes);
    }

    private String safeFilenamePart(String value) {
        return value.trim().replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
