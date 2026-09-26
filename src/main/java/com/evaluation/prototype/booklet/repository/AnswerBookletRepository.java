package com.evaluation.prototype.booklet.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.evaluation.prototype.booklet.entity.AnswerBooklet;
import com.evaluation.prototype.common.enums.AnswerBookletStatus;

public interface AnswerBookletRepository extends JpaRepository<AnswerBooklet, UUID> {
    Optional<AnswerBooklet> findByBookletNumber(String bookletNumber);

    long countByStatus(AnswerBookletStatus status);
}
