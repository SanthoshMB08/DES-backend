package com.evaluation.prototype.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.evaluation.prototype.user.entity.Permission;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {
    Optional<Permission> findByName(String name);

    boolean existsByName(String name);
}
