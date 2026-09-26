package com.evaluation.prototype.user.repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.evaluation.prototype.common.enums.UserStatus;
import com.evaluation.prototype.user.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByDeletedAtIsNull();

    long countByStatusAndDeletedAtIsNull(UserStatus status);

    long countByRoles_NameAndDeletedAtIsNull(String roleName);

    List<User> findByManagerId(UUID managerId);

    long countByManagerId(UUID managerId);

    List<User> findByRoles_NameAndSubjectId(String roleName, UUID subjectId);

    List<User> findByRoles_NameAndAvailableTrueAndSubjectId(String roleName, UUID subjectId);

    // Every BO/Reviewer/Evaluator scoped to a subject (used to deactivate the whole chain on close).
    List<User> findBySubjectId(UUID subjectId);
}
