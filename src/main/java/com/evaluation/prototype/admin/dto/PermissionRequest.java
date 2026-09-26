package com.evaluation.prototype.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record PermissionRequest(
        @NotBlank String name,
        String description,
        Boolean enabled) {
}
