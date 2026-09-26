package com.evaluation.prototype.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.evaluation.prototype.user.entity.Role;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByName(String name);

    boolean existsByName(String name);
}
