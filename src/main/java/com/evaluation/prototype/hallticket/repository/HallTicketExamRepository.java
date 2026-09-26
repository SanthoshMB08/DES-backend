package com.evaluation.prototype.hallticket.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.evaluation.prototype.hallticket.entity.HallTicketExam;

public interface HallTicketExamRepository extends JpaRepository<HallTicketExam, UUID> {
    Optional<HallTicketExam> findByHallTicket_IdAndExamDateAndQpCode(UUID hallTicketId, LocalDate examDate, String qpCode);

    List<HallTicketExam> findByHallTicket_IdOrderByExamDateAsc(UUID hallTicketId);

    /**
     * All exams scheduled for this register number on a given date, across every
     * hall ticket (i.e. every semester) they have - not scoped to one semester,
     * since the barcode-first invigilator flow doesn't ask the invigilator to pick
     * one upfront. Normally resolves to exactly one row; more than one means the
     * student has two exams the same day (e.g. a regular paper plus a backlog
     * paper) and the invigilator needs to pick.
     */
    List<HallTicketExam> findByHallTicket_RegisterNumberIgnoreCaseAndExamDate(String registerNumber, LocalDate examDate);

    /** The same exam located by register number instead of by hall-ticket id. */
    Optional<HallTicketExam> findByHallTicket_RegisterNumberIgnoreCaseAndExamDateAndQpCode(
            String registerNumber, LocalDate examDate, String qpCode);
}
