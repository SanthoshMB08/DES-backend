package com.evaluation.prototype.academic.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.evaluation.prototype.academic.entity.Batch;

public interface BatchRepository extends JpaRepository<Batch, UUID> {
    Optional<Batch> findByProgramIdAndAdmissionYear(UUID programId, String admissionYear);

    List<Batch> findByProgramIdOrderByAdmissionYearDesc(UUID programId);

    List<Batch> findAllByOrderByAdmissionYearDesc();
}
