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

import com.evaluation.prototype.admin.dto.RoleRequest;
import com.evaluation.prototype.admin.dto.RoleResponse;
import com.evaluation.prototype.admin.service.AdminRoleService;
import com.evaluation.prototype.auth.service.CurrentUserService;
import com.evaluation.prototype.user.entity.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/roles")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoleController {

    private final AdminRoleService adminRoleService;
    private final CurrentUserService currentUserService;

    public AdminRoleController(AdminRoleService adminRoleService, CurrentUserService currentUserService) {
        this.adminRoleService = adminRoleService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public RoleResponse create(@Valid @RequestBody RoleRequest request) {
        return RoleResponse.from(adminRoleService.create(request, actor()));
    }

    @GetMapping
    public List<RoleResponse> list() {
        return adminRoleService.list().stream().map(RoleResponse::from).toList();
    }

    @PutMapping("/{id}")
    public RoleResponse update(@PathVariable UUID id, @Valid @RequestBody RoleRequest request) {
        return RoleResponse.from(adminRoleService.update(id, request, actor()));
    }

    @DeleteMapping("/{id}")
    public RoleResponse disable(@PathVariable UUID id) {
        return RoleResponse.from(adminRoleService.disable(id, actor()));
    }

    private User actor() {
        return currentUserService.getCurrentUser().orElse(null);
    }
}
