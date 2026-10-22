package com.evaluation.prototype.academic.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.evaluation.prototype.academic.entity.AcademicSubject;

public interface AcademicSubjectRepository extends JpaRepository<AcademicSubject, UUID> {
    List<AcademicSubject> findBySyllabusIdAndSemesterOrderBySubjectCodeAsc(UUID syllabusId, String semester);

    List<AcademicSubject> findByQpCodeIgnoreCaseAndSemesterOrderByUpdatedAtDesc(String qpCode, String semester);

    @Query("SELECT CASE WHEN COUNT(subject) > 0 THEN true ELSE false END FROM AcademicSubject subject "
            + "WHERE LOWER(subject.qpCode) = LOWER(:qpCode) AND LOWER(subject.semester) = LOWER(:semester)")
    boolean existsByQpCodeAndSemester(@Param("qpCode") String qpCode, @Param("semester") String semester);

    boolean existsByQpCodeIgnoreCase(String qpCode);

    @Query("""
            SELECT CASE WHEN COUNT(subject) > 0 THEN true ELSE false END
            FROM AcademicSubject subject
            WHERE subject.syllabus.id = :syllabusId
              AND LOWER(subject.semester) = LOWER(:semester)
              AND LOWER(subject.qpCode) = LOWER(:qpCode)
              AND (:excludedSubjectId IS NULL OR subject.id <> :excludedSubjectId)
            """)
    boolean existsBySyllabusAndSemesterAndQpCode(
            @Param("syllabusId") UUID syllabusId,
            @Param("semester") String semester,
            @Param("qpCode") String qpCode,
            @Param("excludedSubjectId") UUID excludedSubjectId);

    long countByCurriculumId(UUID curriculumId);

    List<AcademicSubject> findByCurriculumIdOrderBySubjectCodeAsc(UUID curriculumId);

    List<AcademicSubject> findBySyllabusIdAndSemester(UUID syllabusId, String semester);

    @Query("""
            SELECT subject
            FROM AcademicSubject subject
            JOIN subject.syllabus syllabus
            LEFT JOIN subject.curriculum curriculum
            LEFT JOIN curriculum.program curriculumProgram
            LEFT JOIN syllabus.program syllabusProgram
            WHERE subject.semester = :semester
              AND (LOWER(curriculumProgram.code) = LOWER(:course)
                   OR LOWER(curriculumProgram.name) = LOWER(:course)
                   OR LOWER(syllabusProgram.code) = LOWER(:course)
                   OR LOWER(syllabusProgram.name) = LOWER(:course))
            ORDER BY subject.subjectCode ASC
            """)
    List<AcademicSubject> findForMarksLookup(
            @Param("course") String course,
            @Param("semester") String semester);

    @Query("""
            SELECT subject
            FROM AcademicSubject subject
            WHERE subject.semester = :semester
            ORDER BY subject.subjectCode ASC
            """)
    List<AcademicSubject> findAllForMarksLookup(@Param("semester") String semester);
}
