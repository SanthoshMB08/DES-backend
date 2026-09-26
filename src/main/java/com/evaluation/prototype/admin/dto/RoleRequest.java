package com.evaluation.prototype.admin.dto;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public record RoleRequest(
        @NotBlank String name,
        String description,
        Boolean enabled,
        Set<UUID> permissionIds) {
}
