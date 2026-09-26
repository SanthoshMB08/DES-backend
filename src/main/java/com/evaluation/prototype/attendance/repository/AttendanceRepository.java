package com.evaluation.prototype.attendance.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.evaluation.prototype.attendance.entity.Attendance;
import com.evaluation.prototype.common.enums.AttendanceStatus;

// JpaSpecificationExecutor lets search() combine the optional filters instead of
// picking one and discarding the rest.
public interface AttendanceRepository extends JpaRepository<Attendance, UUID>, JpaSpecificationExecutor<Attendance> {
    Optional<Attendance> findByRegisterNumberAndExamDateAndQpCode(String registerNumber, LocalDate examDate, String qpCode);

    boolean existsByAnswerBookletNumber(String answerBookletNumber);

    boolean existsByAnswerBookletNumberAndIdNot(String answerBookletNumber, UUID id);

    List<Attendance> findByRegisterNumber(String registerNumber);

    List<Attendance> findByExamDateAndQpCode(LocalDate examDate, String qpCode);

    List<Attendance> findByStatus(AttendanceStatus status);

    long countByStatus(AttendanceStatus status);

    List<Attendance> findByRegisterNumberAndStudentIsNull(String registerNumber);

    Optional<Attendance> findByAnswerBookletNumber(String answerBookletNumber);
}
