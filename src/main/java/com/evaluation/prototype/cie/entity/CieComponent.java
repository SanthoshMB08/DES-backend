package com.evaluation.prototype.cie.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.evaluation.prototype.user.entity.User;
import com.evaluation.prototype.valuation.entity.Subject;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** One assessment component of a subject's internal marks, e.g. "Test 1" out of 20. */
@Entity
@Table(name = "cie_component")
public class CieComponent {

    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(nullable = false)
    private String name;

    @Column(name = "max_marks", nullable = false)
    private BigDecimal maxMarks;

    @Column(nullable = false)
    private int sequence = 0;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(BigDecimal maxMarks) {
        this.maxMarks = maxMarks;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
