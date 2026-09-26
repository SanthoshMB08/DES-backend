package com.evaluation.prototype.academic.entity;

import java.time.Instant;
import java.util.UUID;

import com.evaluation.prototype.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/** A specific admission cohort of a Program (e.g. "BCA, admitted 2023"). Students belong to a Batch. */
@Entity
@Table(name = "batch")
public class Batch {

    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @Column(name = "admission_year", nullable = false)
    private String admissionYear;

    /** Display label, e.g. "BCA 2023-26" - derivable from program+year+duration, stored for convenience. */
    private String label;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

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

    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
    }

    public String getAdmissionYear() {
        return admissionYear;
    }

    public void setAdmissionYear(String admissionYear) {
        this.admissionYear = admissionYear;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
