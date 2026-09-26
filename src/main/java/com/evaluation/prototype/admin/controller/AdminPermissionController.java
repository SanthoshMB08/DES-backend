package com.evaluation.prototype.admin.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.evaluation.prototype.admin.dto.PermissionRequest;
import com.evaluation.prototype.admin.dto.PermissionResponse;
import com.evaluation.prototype.admin.service.AdminPermissionService;
import com.evaluation.prototype.auth.service.CurrentUserService;
import com.evaluation.prototype.user.entity.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/permissions")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPermissionController {

    private final AdminPermissionService adminPermissionService;
    private final CurrentUserService currentUserService;

    public AdminPermissionController(
            AdminPermissionService adminPermissionService,
            CurrentUserService currentUserService) {
        this.adminPermissionService = adminPermissionService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public PermissionResponse create(@Valid @RequestBody PermissionRequest request) {
        return PermissionResponse.from(adminPermissionService.create(request, actor()));
    }

    @GetMapping
    public List<PermissionResponse> list() {
        return adminPermissionService.list().stream().map(PermissionResponse::from).toList();
    }

    @PutMapping("/{id}")
    public PermissionResponse update(@PathVariable UUID id, @Valid @RequestBody PermissionRequest request) {
        return PermissionResponse.from(adminPermissionService.update(id, request, actor()));
    }

    @DeleteMapping("/{id}")
    public PermissionResponse disable(@PathVariable UUID id) {
        return PermissionResponse.from(adminPermissionService.disable(id, actor()));
    }

    private User actor() {
        return currentUserService.getCurrentUser().orElse(null);
    }
}
