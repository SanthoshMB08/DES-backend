package com.evaluation.prototype.valuation.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.evaluation.prototype.valuation.entity.ValuationScript;
import com.evaluation.prototype.valuation.enums.ValuationStatus;

public interface ValuationScriptRepository extends JpaRepository<ValuationScript, UUID> {

    List<ValuationScript> findByBundleId(UUID bundleId);

    List<ValuationScript> findByCreatedById(UUID createdById);

    List<ValuationScript> findByStatus(ValuationStatus status);

    Optional<ValuationScript> findByAnswerBookletId(String answerBookletId);

    List<ValuationScript> findBySubjectId(UUID subjectId);

    List<ValuationScript> findBySubjectIdAndBundleIsNull(UUID subjectId);

    long countBySubjectIdAndBundleIsNull(UUID subjectId);

    Optional<ValuationScript> findFirstByCandidateIdIgnoreCaseAndQpCodeIgnoreCase(String candidateId, String qpCode);
}
