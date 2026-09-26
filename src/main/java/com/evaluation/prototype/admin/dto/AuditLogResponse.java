package com.evaluation.prototype.admin.dto;

import java.time.Instant;
import java.util.UUID;

import com.evaluation.prototype.audit.entity.AuditLog;

public record AuditLogResponse(
        UUID id,
        String actorUsername,
        String entityType,
        UUID entityId,
        String action,
        String oldValues,
        String newValues,
        Instant createdAt) {

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActorUser() == null ? null : log.getActorUser().getUsername(),
                log.getEntityType(),
                log.getEntityId(),
                log.getAction(),
                log.getOldValues(),
                log.getNewValues(),
                log.getCreatedAt());
    }
}
