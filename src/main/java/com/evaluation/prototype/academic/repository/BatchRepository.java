package com.evaluation.prototype.academic.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.evaluation.prototype.academic.entity.Batch;
import com.evaluation.prototype.academic.dto.BatchResponse;

public interface BatchRepository extends JpaRepository<Batch, UUID> {
    Optional<Batch> findByProgramIdAndAdmissionYear(UUID programId, String admissionYear);

    List<Batch> findByProgramIdOrderByAdmissionYearDesc(UUID programId);

    List<Batch> findAllByOrderByAdmissionYearDesc();

    long countByProgramId(UUID programId);

    @Query("""
            SELECT new com.evaluation.prototype.academic.dto.BatchResponse(
                batch.id, batch.admissionYear, batch.label, program.id, program.name, 0)
            FROM Batch batch
            JOIN batch.program program
            ORDER BY batch.admissionYear DESC, program.name ASC
            """)
    List<BatchResponse> findAllImportOptions();
}
