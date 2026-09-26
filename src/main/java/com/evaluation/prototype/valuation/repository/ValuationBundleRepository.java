package com.evaluation.prototype.valuation.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.evaluation.prototype.valuation.entity.ValuationBundle;
import com.evaluation.prototype.valuation.enums.ValuationStatus;

public interface ValuationBundleRepository extends JpaRepository<ValuationBundle, UUID> {

    Optional<ValuationBundle> findByBundleCode(String bundleCode);

    List<ValuationBundle> findByStatus(ValuationStatus status);

    List<ValuationBundle> findByValuatorId(UUID valuatorId);

    List<ValuationBundle> findByReviewerIdOrStatus(UUID reviewerId, ValuationStatus status);

    List<ValuationBundle> findByStatusIn(List<ValuationStatus> statuses);

    List<ValuationBundle> findBySubjectId(UUID subjectId);

    // Reviewer's view: bundles assigned to any evaluator that this reviewer created.
    List<ValuationBundle> findByValuator_ManagerId(UUID managerId);

    long countBySubjectId(UUID subjectId);
}
