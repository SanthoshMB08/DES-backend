package com.evaluation.prototype.valuation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.evaluation.prototype.valuation.dto.BookletLookupResponse;
import com.evaluation.prototype.valuation.dto.ScriptCreateRequest;
import com.evaluation.prototype.valuation.dto.ScriptResponse;
import com.evaluation.prototype.valuation.dto.ScannerBookletSubmitResponse;
import com.evaluation.prototype.valuation.dto.ScannerRegistrationResponse;
import com.evaluation.prototype.valuation.service.ScannerBookletService;
import com.evaluation.prototype.valuation.service.ValuationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/scanner")
@PreAuthorize("hasAnyRole('ADMIN','SCANNER','SUPPORT_STAFF')")
public class ScannerController {

    private final ValuationService valuationService;
    private final ScannerBookletService scannerBookletService;

    public ScannerController(ValuationService valuationService, ScannerBookletService scannerBookletService) {
        this.valuationService = valuationService;
        this.scannerBookletService = scannerBookletService;
    }

    @GetMapping("/registrations")
    public List<ScannerRegistrationResponse> findRegistrations(
            @RequestParam String usn,
            @RequestParam String qpCode,
            @RequestParam(required = false) String examYearMonth) {
        return scannerBookletService.findRegistrations(usn, qpCode, examYearMonth);
    }

    @PostMapping("/booklets/submit")
    public ScannerBookletSubmitResponse submitBooklet(
            @RequestParam UUID registrationId,
            @RequestParam String bookletNumber,
            @RequestParam("file") MultipartFile file) {
        return scannerBookletService.submit(registrationId, bookletNumber, file);
    }

    @GetMapping("/booklets/{answerBookletId}")
    public BookletLookupResponse lookupBooklet(@PathVariable String answerBookletId) {
        return valuationService.lookupBooklet(answerBookletId);
    }

    @PostMapping("/scripts")
    public ScriptResponse createScript(@Valid @RequestBody ScriptCreateRequest request) {
        return valuationService.createScript(request);
    }

    @GetMapping("/scripts")
    public List<ScriptResponse> myScripts() {
        return valuationService.listMyScannedScripts();
    }

    /** Attaches the actual scanned PDF to a script record the scanner just created. */
    @PostMapping("/scripts/{scriptId}/file")
    public ScriptResponse uploadFile(@PathVariable UUID scriptId, @RequestParam("file") MultipartFile file) {
        return valuationService.attachScannedFile(scriptId, file);
    }

    /** Lets the scanner preview the PDF they uploaded. */
    @GetMapping("/scripts/{scriptId}/file")
    public ResponseEntity<byte[]> getFile(@PathVariable UUID scriptId) {
        byte[] bytes = valuationService.getScriptFile(scriptId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(bytes);
    }

    @GetMapping("/scripts/{scriptId}/corrected-file")
    public ResponseEntity<byte[]> getCorrectedFile(@PathVariable UUID scriptId) {
        byte[] bytes = valuationService.getCorrectedScriptFile(scriptId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(bytes);
    }
}
