package com.evaluation.prototype.audit.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.evaluation.prototype.audit.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {
    List<AuditLog> findTop10ByOrderByCreatedAtDesc();

    long countByCreatedAtBetween(Instant start, Instant end);
}
