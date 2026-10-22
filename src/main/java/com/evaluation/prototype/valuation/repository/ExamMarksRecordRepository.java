package com.evaluation.prototype.valuation.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.evaluation.prototype.valuation.entity.ExamMarksRecord;

public interface ExamMarksRecordRepository extends JpaRepository<ExamMarksRecord, UUID> {
    Optional<ExamMarksRecord> findByBooklet_Id(UUID bookletId);
}
