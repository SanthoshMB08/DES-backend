package com.evaluation.prototype.cie.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.evaluation.prototype.student.entity.Student;
import com.evaluation.prototype.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/** One student's marks for one CIE component. A student enters the CIE sheet the moment their first mark is entered. */
@Entity
@Table(name = "cie_mark")
public class CieMark {

    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "component_id", nullable = false)
    private CieComponent component;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "marks_obtained", nullable = false)
    private BigDecimal marksObtained;

    @ManyToOne
    @JoinColumn(name = "entered_by")
    private User enteredBy;

    @Column(name = "entered_at", nullable = false)
    private Instant enteredAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        enteredAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public CieComponent getComponent() {
        return component;
    }

    public void setComponent(CieComponent component) {
        this.component = component;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public BigDecimal getMarksObtained() {
        return marksObtained;
    }

    public void setMarksObtained(BigDecimal marksObtained) {
        this.marksObtained = marksObtained;
    }

    public User getEnteredBy() {
        return enteredBy;
    }

    public void setEnteredBy(User enteredBy) {
        this.enteredBy = enteredBy;
    }

    public Instant getEnteredAt() {
        return enteredAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
