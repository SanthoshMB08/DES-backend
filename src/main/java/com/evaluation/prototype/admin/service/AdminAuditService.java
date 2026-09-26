package com.evaluation.prototype.admin.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.evaluation.prototype.audit.entity.AuditLog;
import com.evaluation.prototype.admin.dto.AuditLogResponse;
import com.evaluation.prototype.audit.repository.AuditLogRepository;
import com.evaluation.prototype.common.exception.ApiException;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import lombok.NonNull;
@Service
public class AdminAuditService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogRepository auditLogRepository;

    public AdminAuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(String username, LocalDate fromDate, LocalDate toDate, String action,
            String registerNumber, String qpCode, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        return auditLogRepository.findAll((Specification<AuditLog>) (root, query, builder) -> {
            Predicate predicate = builder.conjunction();
            if (StringUtils.hasText(username)) {
                var actor = root.join("actorUser", JoinType.LEFT);
                predicate = builder.and(predicate,
                        builder.like(builder.lower(actor.get("username")), "%" + username.toLowerCase() + "%"));
            }
            if (fromDate != null) {
                predicate = builder.and(predicate,
                        builder.greaterThanOrEqualTo(root.get("createdAt"),
                                fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            }
            if (toDate != null) {
                predicate = builder.and(predicate,
                        builder.lessThan(root.get("createdAt"),
                                toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            }
            if (StringUtils.hasText(action)) {
                predicate = builder.and(predicate,
                        builder.like(builder.lower(root.get("action")), "%" + action.toLowerCase() + "%"));
            }
            if (StringUtils.hasText(registerNumber)) {
                predicate = builder.and(predicate,
                        builder.equal(builder.upper(root.get("registerNumber")), registerNumber.toUpperCase()));
            }
            if (StringUtils.hasText(qpCode)) {
                predicate = builder.and(predicate,
                        builder.equal(builder.upper(root.get("qpCode")), qpCode.toUpperCase()));
            }
            return predicate;
        }, pageRequest).map(AuditLogResponse::from);
    }

    @Transactional(readOnly = true)
    public AuditLogResponse get(@NonNull UUID id) {
        return AuditLogResponse.from(auditLogRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Audit log not found")));
    }

}
