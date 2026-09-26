package com.evaluation.prototype.admin.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.evaluation.prototype.admin.dto.PermissionRequest;
import com.evaluation.prototype.audit.service.AuditService;
import com.evaluation.prototype.common.exception.ApiException;
import com.evaluation.prototype.user.entity.Permission;
//import com.evaluation.prototype.user.entity.Role;
import com.evaluation.prototype.user.entity.User;
import com.evaluation.prototype.user.repository.PermissionRepository;
import com.evaluation.prototype.user.repository.RoleRepository;
import lombok.NonNull;
@Service
public class AdminPermissionService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final AuditService auditService;

    public AdminPermissionService(
            PermissionRepository permissionRepository,
            RoleRepository roleRepository,
            AuditService auditService) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Permission create(PermissionRequest request, User actor) {
        String name = normalizePermission(request.name());
        if (permissionRepository.existsByName(name)) {
            throw new ApiException(HttpStatus.CONFLICT, "Permission already exists");
        }
        Permission permission = new Permission();
        apply(permission, request, name);
        Permission saved = permissionRepository.save(permission);
        grantToAdmin(saved);
        auditService.record(actor, "Permission", saved.getId(), "CREATE_PERMISSION", null,
                "{\"name\":\"%s\"}".formatted(name));
        return saved;
    }

    @Transactional
    public Permission update(UUID id, @NonNull PermissionRequest request, User actor) {
        Permission permission = get(id);
        String name = normalizePermission(request.name());
        permissionRepository.findByName(name)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Permission already exists");
                });
        apply(permission, request, name);
        Permission saved = permissionRepository.save(permission);
        grantToAdmin(saved);
        auditService.record(actor, "Permission", saved.getId(), "UPDATE_PERMISSION", null,
                "{\"name\":\"%s\"}".formatted(name));
        return saved;
    }

    @Transactional
    public Permission disable(UUID id, User actor) {
        Permission permission = get(id);
        permission.setEnabled(false);
        Permission saved = permissionRepository.save(permission);
        auditService.record(actor, "Permission", saved.getId(), "DISABLE_PERMISSION", null,
                "{\"name\":\"%s\"}".formatted(saved.getName()));
        return saved;
    }

    public List<Permission> list() {
        return permissionRepository.findAll();
    }

    private Permission get(@NonNull UUID id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Permission not found"));
    }

    private void apply(Permission permission, PermissionRequest request, String name) {
        permission.setName(name);
        permission.setDescription(request.description());
        permission.setEnabled(request.enabled() == null || request.enabled());
    }

    private void grantToAdmin(Permission permission) {
        roleRepository.findByName("ROLE_ADMIN").ifPresent(admin -> {
            admin.getPermissions().add(permission);
            roleRepository.save(admin);
        });
    }

    private String normalizePermission(String name) {
        return name.trim().toUpperCase();
    }
}
