package com.evaluation.prototype.auth.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.evaluation.prototype.audit.service.AuditService;
import com.evaluation.prototype.auth.dto.ChangePasswordRequest;
import com.evaluation.prototype.auth.dto.LoginRequest;
import com.evaluation.prototype.auth.dto.LoginResponse;
import com.evaluation.prototype.auth.jwt.JwtService;
import com.evaluation.prototype.common.exception.ApiException;
import com.evaluation.prototype.user.entity.User;
import com.evaluation.prototype.user.repository.UserRepository;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AuthService(
            AuthenticationManager authenticationManager,
            CustomUserDetailsService userDetailsService,
            JwtService jwtService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    private static final int MAX_FAILED_ATTEMPTS = 5;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (BadCredentialsException exception) {
            int attempts = recordFailedLogin(request.username());
            int remaining = MAX_FAILED_ATTEMPTS - attempts;
            String message = remaining > 0 && remaining <= 2
                    ? "Invalid username or password. " + remaining + " attempt" + (remaining == 1 ? "" : "s")
                            + " remaining before this account is locked."
                    : "Invalid username or password";
            throw new ApiException(HttpStatus.UNAUTHORIZED, message);
        }
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
        User user = userRepository.findByUsername(request.username()).orElseThrow();
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
        String token = jwtService.generateToken(userDetails);
        List<String> roles = user.getRoles().stream().map(role -> role.getName()).toList();
        auditService.record(user, "User", user.getId(), "LOGIN_SUCCESS", null, "{\"username\":\"%s\"}".formatted(user.getUsername()));
        return new LoginResponse("Bearer", token, jwtService.getExpirationSeconds(), user.getUsername(), roles,
                user.isForcePasswordChange());
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        validateStrongPassword(request.newPassword());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setForcePasswordChange(false);
        userRepository.save(user);
        auditService.record(user, "User", user.getId(), "CHANGE_PASSWORD", null, "{\"username\":\"%s\"}".formatted(username));
    }

    public void validateStrongPassword(String password) {
        if (password == null || password.length() < 8
                || !password.matches(".*[A-Z].*")
                || !password.matches(".*[a-z].*")
                || !password.matches(".*\\d.*")) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Password must be at least 8 characters and include uppercase, lowercase, and number");
        }
    }

    /** @return the account's updated failed-attempt count, or 0 if no such username exists. */
    private int recordFailedLogin(String username) {
        return userRepository.findByUsername(username).map(user -> {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.lock();
            }
            userRepository.save(user);
            auditService.record(user, "User", user.getId(), "LOGIN_FAILED", null,
                    "{\"username\":\"%s\",\"attempts\":%d}".formatted(username, user.getFailedLoginAttempts()));
            return user.getFailedLoginAttempts();
        }).orElse(0);
    }
}
