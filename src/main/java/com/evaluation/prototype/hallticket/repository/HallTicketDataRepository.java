package com.evaluation.prototype.hallticket.repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.evaluation.prototype.hallticket.entity.HallTicketData;

public interface HallTicketDataRepository extends JpaRepository<HallTicketData, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO hall_ticket_data (
                id, student_id, usn, qp_code, exam_year_month, created_by, created_at, updated_at
            )
            VALUES (
                :id, :studentId, :usn, :qpCode, :examYearMonth, :createdBy, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            ON CONFLICT (qp_code, usn, exam_year_month) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("studentId") UUID studentId,
            @Param("usn") String usn,
            @Param("qpCode") String qpCode,
            @Param("examYearMonth") String examYearMonth,
            @Param("createdBy") UUID createdBy);

    Optional<HallTicketData> findByQpCodeIgnoreCaseAndUsnIgnoreCaseAndExamYearMonth(
            String qpCode, String usn, String examYearMonth);

    @EntityGraph(attributePaths = { "student", "booklet" })
    List<HallTicketData> findAllByUsnIgnoreCaseAndQpCodeIgnoreCaseOrderByExamYearMonthDesc(
            String usn, String qpCode);

    Optional<HallTicketData> findByBooklet_Id(UUID bookletId);

    Optional<HallTicketData> findByBooklet_BookletNumber(String bookletNumber);
}
