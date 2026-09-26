package com.evaluation.prototype.audit.service;

import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.evaluation.prototype.audit.entity.AuditLog;
import com.evaluation.prototype.audit.repository.AuditLogRepository;
import com.evaluation.prototype.user.entity.User;

@Service
public class AuditService {

    private static final Pattern REGISTER_NUMBER_PATTERN = Pattern.compile("\"registerNumber\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern QP_CODE_PATTERN = Pattern.compile("\"qpCode\"\\s*:\\s*\"([^\"]+)\"");

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(User actor, String entityType, UUID entityId, String action, String oldValues, String newValues) {
        AuditLog log = new AuditLog();
        log.setActorUser(actor);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setOldValues(oldValues);
        log.setNewValues(newValues);
        log.setRegisterNumber(extract(REGISTER_NUMBER_PATTERN, oldValues, newValues));
        log.setQpCode(extract(QP_CODE_PATTERN, oldValues, newValues));
        auditLogRepository.save(log);
    }

    private String extract(Pattern pattern, String oldValues, String newValues) {
        String joined = (oldValues == null ? "" : oldValues) + " " + (newValues == null ? "" : newValues);
        var matcher = pattern.matcher(joined);
        return matcher.find() ? matcher.group(1) : null;
    }
}
