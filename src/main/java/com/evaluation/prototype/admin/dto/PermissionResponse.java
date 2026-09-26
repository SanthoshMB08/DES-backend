package com.evaluation.prototype.admin.dto;

import java.util.UUID;

import com.evaluation.prototype.user.entity.Permission;

public record PermissionResponse(
        UUID id,
        String name,
        String description,
        boolean enabled) {

    public static PermissionResponse from(Permission permission) {
        return new PermissionResponse(permission.getId(), permission.getName(), permission.getDescription(),
                permission.isEnabled());
    }
}
