package com.evaluation.prototype.cie.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.evaluation.prototype.cie.entity.CieComponent;

public interface CieComponentRepository extends JpaRepository<CieComponent, UUID> {
    List<CieComponent> findBySubjectIdOrderBySequenceAsc(UUID subjectId);

    Optional<CieComponent> findBySubjectIdAndNameIgnoreCase(UUID subjectId, String name);
}
