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
import org.springframework.web.bind.annotation.RestController;

import com.evaluation.prototype.valuation.dto.AssignBundleRequest;
import com.evaluation.prototype.valuation.dto.BundleResponse;
import com.evaluation.prototype.valuation.dto.CreateSubordinateRequest;
import com.evaluation.prototype.valuation.dto.ScriptResponse;
import com.evaluation.prototype.valuation.dto.SubordinateResponse;
import com.evaluation.prototype.valuation.service.HierarchyService;
import com.evaluation.prototype.valuation.service.ValuationService;

import jakarta.validation.Valid;

/**
 * Subject Chief: appointed by a BO to run one subject's evaluation end to end. Appoints the
 * Reviewer for that subject (who in turn appoints up to 4 Evaluators), groups scanned answer
 * sheets into bundles of 20, and assigns bundles to available Evaluators.
 *
 * This is the same job the BO used to do directly in the previous version of this API - it
 * has moved here because a BO now oversees an entire Course (many subjects), so a subject
 * needs its own Chief to actually run day-to-day evaluation for it.
 */
@RestController
@RequestMapping("/api/chief")
@PreAuthorize("hasAnyRole('ADMIN','CHIEF')")
public class ChiefController {

    private final ValuationService valuationService;
    private final HierarchyService hierarchyService;

    public ChiefController(ValuationService valuationService, HierarchyService hierarchyService) {
        this.valuationService = valuationService;
        this.hierarchyService = hierarchyService;
    }

    // ---------- Reviewer ----------

    @PostMapping("/reviewers")
    public SubordinateResponse createReviewer(@Valid @RequestBody CreateSubordinateRequest request) {
        return hierarchyService.createReviewer(request);
    }

    @GetMapping("/reviewers")
    public List<SubordinateResponse> reviewers() {
        return hierarchyService.listReviewers();
    }

    @GetMapping("/evaluators")
    public List<SubordinateResponse> evaluators() {
        return hierarchyService.listEvaluatorsUnderBo();
    }

    // ---------- Answer sheets / bundles ----------

    @GetMapping("/scripts/unbundled")
    public List<ScriptResponse> unbundledScripts() {
        return valuationService.listUnbundledScripts();
    }

    @PostMapping("/bundles/form")
    public BundleResponse formBundle() {
        return valuationService.formBundle();
    }

    @GetMapping("/bundles")
    public List<BundleResponse> bundles() {
        return valuationService.listBoBundles();
    }

    @GetMapping("/bundles/{bundleId}/scripts")
    public List<ScriptResponse> bundleScripts(@PathVariable UUID bundleId) {
        return valuationService.listBundleScripts(bundleId);
    }

    @PostMapping("/bundles/{bundleId}/assign")
    public BundleResponse assignBundle(@PathVariable UUID bundleId, @Valid @RequestBody AssignBundleRequest request) {
        return valuationService.assignBundle(bundleId, request.evaluatorId());
    }

    /** View-only: lets the Chief spot-check a script's scanned/annotated copy. */
    @GetMapping("/scripts/{scriptId}/file")
    public ResponseEntity<byte[]> getFile(@PathVariable UUID scriptId) {
        byte[] bytes = valuationService.getReviewScriptFile(scriptId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(bytes);
    }
}
