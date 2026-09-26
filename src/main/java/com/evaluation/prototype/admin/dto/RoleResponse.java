package com.evaluation.prototype.admin.dto;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.evaluation.prototype.user.entity.Role;

public record RoleResponse(
        UUID id,
        String name,
        String description,
        boolean enabled,
        Set<String> permissions) {

    public static RoleResponse from(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.isEnabled(),
                role.getPermissions().stream().map(permission -> permission.getName()).collect(Collectors.toSet()));
    }
}
