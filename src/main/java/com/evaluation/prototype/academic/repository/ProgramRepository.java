package com.evaluation.prototype.academic.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.evaluation.prototype.academic.entity.Program;

public interface ProgramRepository extends JpaRepository<Program, UUID> {
    Optional<Program> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<Program> findAllByOrderByNameAsc();

    List<Program> findByDepartmentIdOrderByNameAsc(UUID departmentId);

    long countByDepartmentId(UUID departmentId);
}
