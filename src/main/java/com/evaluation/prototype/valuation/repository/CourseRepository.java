package com.evaluation.prototype.valuation.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.evaluation.prototype.valuation.entity.Course;

public interface CourseRepository extends JpaRepository<Course, UUID> {
    Optional<Course> findByCourseNameIgnoreCaseAndSemester(String courseName, String semester);

    Optional<Course> findByCourseNameIgnoreCaseAndSemesterAndSyllabusYearIgnoreCase(
            String courseName, String semester, String syllabusYear);

    Optional<Course> findByBoId(UUID boId);

    List<Course> findAllByOrderByCreatedAtDesc();
}
