package com.evaluation.prototype.academic.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.evaluation.prototype.academic.entity.Department;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    Optional<Department> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<Department> findAllByOrderByNameAsc();
}
