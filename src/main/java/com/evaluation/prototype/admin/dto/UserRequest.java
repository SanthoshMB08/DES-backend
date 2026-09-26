package com.evaluation.prototype.admin.dto;

import java.util.Set;
import java.util.UUID;

import com.evaluation.prototype.common.enums.UserStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserRequest(
        @NotBlank String fullName,
        @NotBlank String username,
        String password,
        String email,
        String phoneNumber,
        @NotNull UserStatus status,
        Set<UUID> roleIds) {
}
