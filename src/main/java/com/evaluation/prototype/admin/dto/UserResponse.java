package com.evaluation.prototype.admin.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.evaluation.prototype.common.enums.UserStatus;
import com.evaluation.prototype.user.entity.User;

public record UserResponse(
        UUID id,
        String fullName,
        String username,
        String email,
        String phoneNumber,
        UserStatus status,
        boolean forcePasswordChange,
        int failedLoginAttempts,
        Instant lockedAt,
        Instant deletedAt,
        Set<String> roles) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getStatus(),
                user.isForcePasswordChange(),
                user.getFailedLoginAttempts(),
                user.getLockedAt(),
                user.getDeletedAt(),
                user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toSet()));
    }
}
