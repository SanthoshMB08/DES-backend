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

import com.evaluation.prototype.valuation.dto.BundleResponse;
import com.evaluation.prototype.valuation.dto.CreateSubordinateRequest;
import com.evaluation.prototype.valuation.dto.ReviewRequest;
import com.evaluation.prototype.valuation.dto.ScriptResponse;
import com.evaluation.prototype.valuation.dto.SubordinateResponse;
import com.evaluation.prototype.valuation.service.HierarchyService;
import com.evaluation.prototype.valuation.service.ValuationService;

import jakarta.validation.Valid;

/**
 * Reviewer: created by a BO. Creates up to 4 Evaluators and reviews the bundles they evaluate.
 * Approving a script fixes its marks - they cannot be changed afterwards.
 */
@RestController
@RequestMapping("/api/reviewer")
@PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
public class ReviewerController {

    private final ValuationService valuationService;
    private final HierarchyService hierarchyService;

    public ReviewerController(ValuationService valuationService, HierarchyService hierarchyService) {
        this.valuationService = valuationService;
        this.hierarchyService = hierarchyService;
    }

    // ---------- Evaluators ----------

    @PostMapping("/evaluators")
    public SubordinateResponse createEvaluator(@Valid @RequestBody CreateSubordinateRequest request) {
        return hierarchyService.createEvaluator(request);
    }

    @GetMapping("/evaluators")
    public List<SubordinateResponse> evaluators() {
        return hierarchyService.listEvaluators();
    }

    // ---------- Review ----------

    @GetMapping("/bundles")
    public List<BundleResponse> bundles() {
        return valuationService.listReviewerBundles();
    }

    @GetMapping("/bundles/{bundleId}/scripts")
    public List<ScriptResponse> bundleScripts(@PathVariable UUID bundleId) {
        return valuationService.listBundleScripts(bundleId);
    }

    @GetMapping("/scripts/{scriptId}")
    public ScriptResponse script(@PathVariable UUID scriptId) {
        return valuationService.getScript(scriptId);
    }

    @PostMapping("/scripts/{scriptId}/review")
    public ScriptResponse review(@PathVariable UUID scriptId, @Valid @RequestBody ReviewRequest request) {
        return valuationService.reviewScript(scriptId, request);
    }

    /** View-only: lets the reviewer see the evaluator's annotated copy while reviewing. */
    @GetMapping("/scripts/{scriptId}/file")
    public ResponseEntity<byte[]> getFile(@PathVariable UUID scriptId) {
        byte[] bytes = valuationService.getScriptFile(scriptId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(bytes);
    }
}
