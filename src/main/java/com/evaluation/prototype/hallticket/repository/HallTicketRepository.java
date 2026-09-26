package com.evaluation.prototype.hallticket.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.evaluation.prototype.hallticket.entity.HallTicket;

public interface HallTicketRepository extends JpaRepository<HallTicket, UUID> {
    Optional<HallTicket> findByQrHash(String qrHash);

    Optional<HallTicket> findByRegisterNumberAndSemesterIgnoreCase(String registerNumber, String semester);
}
