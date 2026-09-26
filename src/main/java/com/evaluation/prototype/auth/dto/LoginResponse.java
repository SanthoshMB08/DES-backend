package com.evaluation.prototype.auth.dto;

import java.util.List;

public record LoginResponse(
        String tokenType,
        String accessToken,
        long expiresInSeconds,
        String username,
        List<String> roles,
        boolean forcePasswordChange) {
}
