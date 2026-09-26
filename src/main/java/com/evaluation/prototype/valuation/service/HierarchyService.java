package com.evaluation.prototype.valuation.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.evaluation.prototype.auth.service.AuthService;
import com.evaluation.prototype.auth.service.CurrentUserService;
import com.evaluation.prototype.common.exception.ApiException;
import com.evaluation.prototype.user.entity.User;
import com.evaluation.prototype.user.repository.RoleRepository;
import com.evaluation.prototype.user.repository.UserRepository;
import com.evaluation.prototype.valuation.dto.CreateSubordinateRequest;
import com.evaluation.prototype.valuation.dto.SubordinateResponse;
import com.evaluation.prototype.valuation.entity.ValuationBundle;
import com.evaluation.prototype.valuation.enums.ValuationStatus;
import com.evaluation.prototype.valuation.repository.ValuationBundleRepository;

@Service
public class HierarchyService {

    private static final int MAX_EVALUATORS_PER_REVIEWER = 4;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final ValuationBundleRepository bundleRepository;
    private final AuthService authService;

    public HierarchyService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            CurrentUserService currentUserService,
            ValuationBundleRepository bundleRepository,
            AuthService authService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
        this.bundleRepository = bundleRepository;
        this.authService = authService;
    }

    private User currentUser() {
        return currentUserService.getCurrentUser()
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }

    private User createUser(CreateSubordinateRequest request, String roleName, User manager) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "Username already taken: " + request.username());
        }
        authService.validateStrongPassword(request.password());
        var role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Role not seeded: " + roleName));

        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName() != null ? request.fullName() : request.username());
        user.setManager(manager);
        user.setSubjectId(manager.getSubjectId());
        user.getRoles().add(role);
        return userRepository.save(user);
    }

    // ---------- Chief creates Reviewer (formerly done by BO directly) ----------

    @Transactional
    public SubordinateResponse createReviewer(CreateSubordinateRequest request) {
        User chief = currentUser();
        User reviewer = createUser(request, "ROLE_REVIEWER", chief);
        return toResponse(reviewer);
    }

    public List<SubordinateResponse> listReviewers() {
        User chief = currentUser();
        return userRepository.findByManagerId(chief.getId()).stream().map(this::toResponse).toList();
    }

    // ---------- Reviewer creates Evaluator (max 4) ----------

    @Transactional
    public SubordinateResponse createEvaluator(CreateSubordinateRequest request) {
        User reviewer = currentUser();
        long existing = userRepository.countByManagerId(reviewer.getId());
        if (existing >= MAX_EVALUATORS_PER_REVIEWER) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "A reviewer can have at most " + MAX_EVALUATORS_PER_REVIEWER + " evaluators");
        }
        User evaluator = createUser(request, "ROLE_VALUATOR", reviewer);
        return toResponse(evaluator);
    }

    public List<SubordinateResponse> listEvaluators() {
        User reviewer = currentUser();
        return userRepository.findByManagerId(reviewer.getId()).stream().map(this::toResponse).toList();
    }

    /** Chief's view of every evaluator working under the reviewers they created (formerly the BO's view). */
    public List<SubordinateResponse> listEvaluatorsUnderBo() {
        User chief = currentUser();
        List<User> reviewers = userRepository.findByManagerId(chief.getId());
        return reviewers.stream()
                .flatMap(reviewer -> userRepository.findByManagerId(reviewer.getId()).stream())
                .map(this::toResponse)
                .toList();
    }

    // ---------- Evaluator availability ----------

    public SubordinateResponse getSelf() {
        return toResponse(currentUser());
    }

    @Transactional
    public SubordinateResponse toggleAvailability() {
        User evaluator = currentUser();
        boolean hasActiveBundle = !bundleRepository.findByValuatorId(evaluator.getId()).stream()
                .filter(b -> b.getStatus() == ValuationStatus.PENDING)
                .toList().isEmpty();
        if (hasActiveBundle && !evaluator.isAvailable()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "You still have a bundle in progress - finish evaluating it first");
        }
        evaluator.setAvailable(!evaluator.isAvailable());
        evaluator = userRepository.save(evaluator);
        return toResponse(evaluator);
    }

    private SubordinateResponse toResponse(User user) {
        String activeBundleCode = bundleRepository.findByValuatorId(user.getId()).stream()
                .filter(b -> b.getStatus() == ValuationStatus.PENDING)
                .map(ValuationBundle::getBundleCode)
                .findFirst()
                .orElse(null);
        String role = user.getRoles().stream().findFirst().map(r -> r.getName().replace("ROLE_", "")).orElse("");
        return new SubordinateResponse(
                user.getId(), user.getUsername(), user.getFullName(), role,
                user.isAvailable(), user.getSubjectId(), activeBundleCode);
    }
}
