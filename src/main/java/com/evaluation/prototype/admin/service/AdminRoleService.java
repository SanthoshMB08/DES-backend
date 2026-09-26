package com.evaluation.prototype.admin.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.evaluation.prototype.admin.dto.RoleRequest;
import com.evaluation.prototype.audit.service.AuditService;
import com.evaluation.prototype.common.exception.ApiException;
import com.evaluation.prototype.user.entity.Permission;
import com.evaluation.prototype.user.entity.Role;
import com.evaluation.prototype.user.entity.User;
import com.evaluation.prototype.user.repository.PermissionRepository;
import com.evaluation.prototype.user.repository.RoleRepository;

import lombok.NonNull;
@Service
public class AdminRoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AuditService auditService;

    public AdminRoleService(RoleRepository roleRepository, PermissionRepository permissionRepository, AuditService auditService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Role create(RoleRequest request, User actor) {
        String name = normalizeRole(request.name());
        if (roleRepository.existsByName(name)) {
            throw new ApiException(HttpStatus.CONFLICT, "Role already exists");
        }
        Role role = new Role();
        apply(role, request, name);
        Role saved = roleRepository.save(role);
        auditService.record(actor, "Role", saved.getId(), "CREATE_ROLE", null, "{\"name\":\"%s\"}".formatted(name));
        return saved;
    }

    @Transactional
    public Role update(UUID id, RoleRequest request, User actor) {
        Role role = get(id);
        String name = normalizeRole(request.name());
        roleRepository.findByName(name)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Role already exists");
                });
        apply(role, request, name);
        Role saved = roleRepository.save(role);
        auditService.record(actor, "Role", saved.getId(), "UPDATE_ROLE", null, "{\"name\":\"%s\"}".formatted(name));
        return saved;
    }

    @Transactional
    public Role disable(UUID id, User actor) {
        Role role = get(id);
        if ("ROLE_ADMIN".equals(role.getName())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ROLE_ADMIN cannot be disabled");
        }
        role.setEnabled(false);
        Role saved = roleRepository.save(role);
        auditService.record(actor, "Role", saved.getId(), "DISABLE_ROLE", null,
                "{\"name\":\"%s\"}".formatted(saved.getName()));
        return saved;
    }

    public List<Role> list() {
        return roleRepository.findAll();
    }

    private Role get(@NonNull UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Role not found"));
    }

    private void apply(Role role, RoleRequest request, String name) {
        role.setName(name);
        role.setDescription(request.description());
        role.setEnabled(request.enabled() == null || request.enabled());
        role.getPermissions().clear();
        permissionsFrom(request.permissionIds()).forEach(role.getPermissions()::add);
    }

    private List<Permission> permissionsFrom(Set<UUID> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return List.of();
        }
        return permissionRepository.findAllById(permissionIds);
    }

    private String normalizeRole(String name) {
        String normalized = name.trim().toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }
}
