package com.evaluation.prototype.valuation.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.evaluation.prototype.valuation.entity.Subject;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    Optional<Subject> findByQpCode(String qpCode);

    Optional<Subject> findByQpCodeIgnoreCase(String qpCode);

    boolean existsByQpCodeIgnoreCase(String qpCode);

    boolean existsByQpCodeIgnoreCaseAndSemesterIgnoreCase(String qpCode, String semester);

    List<Subject> findByCourseRefId(UUID courseId);

    List<Subject> findByChiefId(UUID chiefId);

}
