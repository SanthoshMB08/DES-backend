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

import com.evaluation.prototype.admin.dto.ResetPasswordResponse;
import com.evaluation.prototype.admin.dto.UserRequest;
import com.evaluation.prototype.admin.dto.UserResponse;
import com.evaluation.prototype.admin.service.AdminUserService;
import com.evaluation.prototype.auth.service.CurrentUserService;
import com.evaluation.prototype.user.entity.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final CurrentUserService currentUserService;

    public AdminUserController(AdminUserService adminUserService, CurrentUserService currentUserService) {
        this.adminUserService = adminUserService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public UserResponse create(@Valid @RequestBody UserRequest request) {
        return UserResponse.from(adminUserService.create(request, actor()));
    }

    @GetMapping
    public List<UserResponse> list() {
        return adminUserService.list().stream().map(UserResponse::from).toList();
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable UUID id) {
        return UserResponse.from(adminUserService.get(id));
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UserRequest request) {
        return UserResponse.from(adminUserService.update(id, request, actor()));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        adminUserService.softDelete(id, actor());
    }

    @PostMapping("/{id}/reset-password")
    public ResetPasswordResponse resetPassword(@PathVariable UUID id) {
        return adminUserService.resetPassword(id, actor());
    }

    @PostMapping("/{id}/unlock")
    public UserResponse unlock(@PathVariable UUID id) {
        return UserResponse.from(adminUserService.unlock(id, actor()));
    }

    private User actor() {
        return currentUserService.getCurrentUser().orElse(null);
    }
}
