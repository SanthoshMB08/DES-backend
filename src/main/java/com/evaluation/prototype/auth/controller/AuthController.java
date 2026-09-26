package com.evaluation.prototype.auth.controller;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.evaluation.prototype.auth.dto.LoginRequest;
import com.evaluation.prototype.auth.dto.LoginResponse;
import com.evaluation.prototype.auth.dto.ChangePasswordRequest;
import com.evaluation.prototype.auth.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public Map<String, String> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(authentication.getName(), request);
        return Map.of("message", "Password changed successfully");
    }

    @PostMapping("/logout")
    public Map<String, String> logout() {
        return Map.of("message", "Logout successful. Discard the client-side JWT token.");
    }
}
