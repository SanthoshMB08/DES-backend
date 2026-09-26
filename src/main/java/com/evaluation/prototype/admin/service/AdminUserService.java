package com.evaluation.prototype.admin.service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.evaluation.prototype.admin.dto.ResetPasswordResponse;
import com.evaluation.prototype.admin.dto.UserRequest;
import com.evaluation.prototype.audit.service.AuditService;
import com.evaluation.prototype.auth.service.AuthService;
import com.evaluation.prototype.common.enums.UserStatus;
import com.evaluation.prototype.common.exception.ApiException;
import com.evaluation.prototype.user.entity.Role;
import com.evaluation.prototype.user.entity.User;
import com.evaluation.prototype.user.repository.RoleRepository;
import com.evaluation.prototype.user.repository.UserRepository;

import lombok.NonNull;
@Service
public class AdminUserService {

    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AdminUserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthService authService,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.auditService = auditService;
    }

    @Transactional
    public User create(UserRequest request, User actor) {
        String username = normalizeUsername(request.username());
        if (userRepository.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (!StringUtils.hasText(request.password())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        authService.validateStrongPassword(request.password());

        User user = new User();
        apply(user, request);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setForcePasswordChange(false);
        User saved = userRepository.save(user);
        auditService.record(actor, "User", saved.getId(), "CREATE_USER", null,
                "{\"username\":\"%s\"}".formatted(saved.getUsername()));
        return saved;
    }

    @Transactional
    public User update(UUID id, UserRequest request, User actor) {
        User user = getActiveOrExisting(id);
        String username = normalizeUsername(request.username());
        userRepository.findByUsername(username)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Username already exists");
                });

        String oldValues = "{\"username\":\"%s\",\"status\":\"%s\"}".formatted(user.getUsername(), user.getStatus());
        apply(user, request);
        user.setUsername(username);
        if (StringUtils.hasText(request.password())) {
            authService.validateStrongPassword(request.password());
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            user.setForcePasswordChange(true);
        }
        User saved = userRepository.save(user);
        auditService.record(actor, "User", saved.getId(), "UPDATE_USER", oldValues,
                "{\"username\":\"%s\",\"status\":\"%s\"}".formatted(saved.getUsername(), saved.getStatus()));
        return saved;
    }

    @Transactional
    public void softDelete(UUID id, User actor) {
        User user = getActiveOrExisting(id);
        user.softDelete(actor);
        userRepository.save(user);
        auditService.record(actor, "User", id, "DELETE_USER", null,
                "{\"username\":\"%s\",\"softDelete\":true}".formatted(user.getUsername()));
    }

    @Transactional
    public User deactivate(UUID id, User actor) {
        User user = getActiveOrExisting(id);
        user.setStatus(UserStatus.INACTIVE);
        User saved = userRepository.save(user);
        auditService.record(actor, "User", id, "DEACTIVATE_USER", null,
                "{\"username\":\"%s\"}".formatted(saved.getUsername()));
        return saved;
    }

    @Transactional
    public ResetPasswordResponse resetPassword(UUID id, User actor) {
        User user = getActiveOrExisting(id);
        String temporaryPassword = generateTemporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setForcePasswordChange(true);
        userRepository.save(user);
        auditService.record(actor, "User", id, "RESET_PASSWORD", null,
                "{\"username\":\"%s\"}".formatted(user.getUsername()));
        return new ResetPasswordResponse(user.getUsername(), temporaryPassword, true);
    }

    @Transactional
    public User unlock(UUID id, User actor) {
        User user = getActiveOrExisting(id);
        user.unlock();
        User saved = userRepository.save(user);
        auditService.record(actor, "User", id, "UNLOCK_USER", null,
                "{\"username\":\"%s\"}".formatted(user.getUsername()));
        return saved;
    }

    public List<User> list() {
        return userRepository.findByDeletedAtIsNull();
    }

    public User get(UUID id) {
        return getActiveOrExisting(id);
    }

    private void apply(User user, UserRequest request) {
        user.setFullName(request.fullName().trim());
        user.setEmail(StringUtils.hasText(request.email()) ? request.email().trim() : null);
        user.setPhoneNumber(StringUtils.hasText(request.phoneNumber()) ? request.phoneNumber().trim() : null);
        user.setStatus(request.status());
        user.getRoles().clear();
        rolesFrom(request.roleIds()).forEach(user.getRoles()::add);
    }

    private List<Role> rolesFrom(Set<UUID> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        return roleRepository.findAllById(roleIds);
    }

    private User getActiveOrExisting(@NonNull UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase();
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder("T1a");
        for (int i = 0; i < 9; i++) {
            password.append(TEMP_PASSWORD_CHARS.charAt(secureRandom.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return password.toString();
    }
}
