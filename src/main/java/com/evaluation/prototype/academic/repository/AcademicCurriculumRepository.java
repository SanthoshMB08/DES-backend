package com.evaluation.prototype.academic.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.evaluation.prototype.academic.entity.AcademicCurriculum;

public interface AcademicCurriculumRepository extends JpaRepository<AcademicCurriculum, UUID> {
    boolean existsByProgramIdAndSyllabusIdAndSemester(UUID programId, UUID syllabusId, String semester);
    Optional<AcademicCurriculum> findByProgramIdAndSyllabusIdAndSemester(UUID programId, UUID syllabusId, String semester);
    List<AcademicCurriculum> findAllByOrderByCreatedAtDesc();
}
