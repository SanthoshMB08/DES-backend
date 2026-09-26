package com.evaluation.prototype.valuation.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.evaluation.prototype.valuation.entity.Subject;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    Optional<Subject> findByQpCode(String qpCode);

    List<Subject> findByCourseRefId(UUID courseId);

    List<Subject> findByChiefId(UUID chiefId);

    List<Subject> findByFacultyId(UUID facultyId);
}
