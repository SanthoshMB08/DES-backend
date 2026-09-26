package com.evaluation.prototype.academic.repository;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository; import com.evaluation.prototype.academic.entity.Syllabus;
public interface SyllabusRepository extends JpaRepository<Syllabus, UUID> {
    List<Syllabus> findByProgramIdOrderByYearDesc(UUID programId);
    List<Syllabus> findByProgramIdIsNullOrProgramIdOrderByYearDesc(UUID programId);
    List<Syllabus> findAllByOrderByYearDesc();
}
