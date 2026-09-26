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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.evaluation.prototype.valuation.dto.BundleResponse;
import com.evaluation.prototype.valuation.dto.ScriptResponse;
import com.evaluation.prototype.valuation.dto.ScriptSubmitRequest;
import com.evaluation.prototype.valuation.dto.SubordinateResponse;
import com.evaluation.prototype.valuation.service.HierarchyService;
import com.evaluation.prototype.valuation.service.ValuationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/valuator")
@PreAuthorize("hasAnyRole('ADMIN','VALUATOR')")
public class ValuatorController {

    private final ValuationService valuationService;
    private final HierarchyService hierarchyService;

    public ValuatorController(ValuationService valuationService, HierarchyService hierarchyService) {
        this.valuationService = valuationService;
        this.hierarchyService = hierarchyService;
    }

    @GetMapping("/me")
    public SubordinateResponse me() {
        return hierarchyService.getSelf();
    }

    @PostMapping("/availability/toggle")
    public SubordinateResponse toggleAvailability() {
        return hierarchyService.toggleAvailability();
    }

    @GetMapping("/bundles")
    public List<BundleResponse> bundles() {
        return valuationService.listValuatorBundles();
    }

    @GetMapping("/bundles/{bundleId}/scripts")
    public List<ScriptResponse> bundleScripts(@PathVariable UUID bundleId) {
        return valuationService.listBundleScripts(bundleId);
    }

    @PostMapping("/bundles/{bundleId}/submit")
    public int submitBundle(@PathVariable UUID bundleId) {
        return valuationService.submitBundle(bundleId);
    }

    @GetMapping("/scripts/{scriptId}")
    public ScriptResponse script(@PathVariable UUID scriptId) {
        return valuationService.getScript(scriptId);
    }

    @PostMapping("/scripts/{scriptId}/submit")
    public ScriptResponse submit(@PathVariable UUID scriptId, @Valid @RequestBody ScriptSubmitRequest request) {
        return valuationService.submitScriptMarks(scriptId, request);
    }

    /** Loads the scanned PDF straight into the evaluator's viewer - never a forced download. */
    @GetMapping("/scripts/{scriptId}/file")
    public ResponseEntity<byte[]> getFile(@PathVariable UUID scriptId) {
        byte[] bytes = valuationService.getScriptFile(scriptId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(bytes);
    }

    /** Evaluator uploads their corrected/annotated copy back, overwriting the same stored file. */
    @PutMapping("/scripts/{scriptId}/file")
    public void replaceFile(@PathVariable UUID scriptId, @RequestParam("file") MultipartFile file) {
        valuationService.replaceScriptFile(scriptId, file);
    }
}
