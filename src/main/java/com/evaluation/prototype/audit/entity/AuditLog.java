package com.evaluation.prototype.audit.entity;

import java.time.Instant;
import java.util.UUID;

import com.evaluation.prototype.user.entity.User;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actorUser;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(nullable = false)
    private String action;

    @Column(name = "old_values", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String oldValues;

    @Column(name = "new_values", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String newValues;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "register_number")
    private String registerNumber;

    @Column(name = "qp_code")
    private String qpCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
    }

    public void setActorUser(User actorUser) {
        this.actorUser = actorUser;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setOldValues(String oldValues) {
        this.oldValues = oldValues;
    }

    public void setNewValues(String newValues) {
        this.newValues = newValues;
    }

    public void setRegisterNumber(String registerNumber) {
        this.registerNumber = registerNumber;
    }

    public void setQpCode(String qpCode) {
        this.qpCode = qpCode;
    }

    public UUID getId() {
        return id;
    }

    public User getActorUser() {
        return actorUser;
    }

    public String getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public String getAction() {
        return action;
    }

    public String getOldValues() {
        return oldValues;
    }

    public String getNewValues() {
        return newValues;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getRegisterNumber() {
        return registerNumber;
    }

    public String getQpCode() {
        return qpCode;
    }
}
