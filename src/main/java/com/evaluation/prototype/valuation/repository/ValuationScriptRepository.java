package com.evaluation.prototype.valuation.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

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

    /**
     * Repairs sheets uploaded before a subject was available or where the QP
     * code contained whitespace/case differences.  These otherwise become
     * invisible to RE, BO and Chief dashboards because those views are
     * intentionally scoped by subject_id.
     */
    @Modifying
    @Query(value = """
            UPDATE valuation_script script
            SET subject_id = subject.id,
                subject = COALESCE(script.subject, subject.subject_name),
                course = COALESCE(script.course, subject.course),
                semester = COALESCE(script.semester, subject.semester)
            FROM subject
            WHERE script.subject_id IS NULL
              AND UPPER(BTRIM(script.qp_code)) = UPPER(BTRIM(subject.qp_code))
            """, nativeQuery = true)
    int linkUnassignedScriptsToSubjects();

    Optional<ValuationScript> findFirstByCandidateIdIgnoreCaseAndQpCodeIgnoreCase(String candidateId, String qpCode);

    Optional<ValuationScript> findFirstByCandidateIdIgnoreCaseAndQpCodeIgnoreCaseOrderByUpdatedAtDesc(
            String candidateId, String qpCode);
}
