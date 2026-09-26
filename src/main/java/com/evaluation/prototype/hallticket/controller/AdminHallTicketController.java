package com.evaluation.prototype.hallticket.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.evaluation.prototype.auth.service.CurrentUserService;
import com.evaluation.prototype.common.exception.ApiException;
import com.evaluation.prototype.hallticket.dto.HallTicketImportRequest;
import com.evaluation.prototype.hallticket.dto.HallTicketImportResponse;
import com.evaluation.prototype.hallticket.service.HallTicketImportService;

import jakarta.validation.Valid;

/**
 * Bulk hall-ticket import - the same endpoint serves two paths: the OCR
 * module can POST here directly once a job finishes (push), or an Admin can
 * upload the exported JSON through the app's own UI (import). Either way
 * the payload shape is identical - see HallTicketImportRequest.
 */
@RestController
@RequestMapping("/api/admin/halltickets")
@PreAuthorize("hasRole('ADMIN')")
public class AdminHallTicketController {

    private final HallTicketImportService hallTicketImportService;
    private final CurrentUserService currentUserService;

    public AdminHallTicketController(HallTicketImportService hallTicketImportService, CurrentUserService currentUserService) {
        this.hallTicketImportService = hallTicketImportService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/import")
    public HallTicketImportResponse importHallTickets(@Valid @RequestBody HallTicketImportRequest request) {
        var actor = currentUserService.getCurrentUser()
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
        return hallTicketImportService.importHallTickets(request, actor);
    }
}
