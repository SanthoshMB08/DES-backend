package com.evaluation.prototype.student.repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.evaluation.prototype.student.entity.Student;

public interface StudentRepository extends JpaRepository<Student, UUID> {
    Optional<Student> findByRegisterNumber(String registerNumber);

    List<Student> findByRegisterNumberContainingIgnoreCase(String registerNumber);

    List<Student> findByBatchIdOrderByRegisterNumberAsc(UUID batchId);

    long countByBatchId(UUID batchId);
}
