package com.evaluation.prototype.valuation.entity;

import java.time.Instant;
import java.util.UUID;

import com.evaluation.prototype.user.entity.User;
import com.evaluation.prototype.valuation.enums.ValuationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "valuation_bundle")
public class ValuationBundle {

    @Id
    private UUID id;

    @Column(name = "bundle_code", nullable = false, unique = true)
    private String bundleCode;

    private String course;
    private String semester;

    @Column(name = "qp_code")
    private String qpCode;

    private String subject;

    @Column(name = "center_code")
    private String centerCode;

    @Column(name = "total_scripts", nullable = false)
    private int totalScripts;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ValuationStatus status = ValuationStatus.PENDING;

    @ManyToOne
    @JoinColumn(name = "valuator_id")
    private User valuator;

    @ManyToOne
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @ManyToOne
    @JoinColumn(name = "boe_id")
    private User boe;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getBundleCode() {
        return bundleCode;
    }

    public void setBundleCode(String bundleCode) {
        this.bundleCode = bundleCode;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getQpCode() {
        return qpCode;
    }

    public void setQpCode(String qpCode) {
        this.qpCode = qpCode;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getCenterCode() {
        return centerCode;
    }

    public void setCenterCode(String centerCode) {
        this.centerCode = centerCode;
    }

    public int getTotalScripts() {
        return totalScripts;
    }

    public void setTotalScripts(int totalScripts) {
        this.totalScripts = totalScripts;
    }

    public void incrementTotalScripts() {
        this.totalScripts = this.totalScripts + 1;
    }

    public ValuationStatus getStatus() {
        return status;
    }

    public void setStatus(ValuationStatus status) {
        this.status = status;
    }

    public User getValuator() {
        return valuator;
    }

    public void setValuator(User valuator) {
        this.valuator = valuator;
    }

    public User getReviewer() {
        return reviewer;
    }

    public void setReviewer(User reviewer) {
        this.reviewer = reviewer;
    }

    public User getBoe() {
        return boe;
    }

    public void setBoe(User boe) {
        this.boe = boe;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(UUID subjectId) {
        this.subjectId = subjectId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
