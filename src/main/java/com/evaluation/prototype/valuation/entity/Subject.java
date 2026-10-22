package com.evaluation.prototype.valuation.entity;

import java.time.Instant;
import java.util.UUID;

import com.evaluation.prototype.user.entity.User;
import com.evaluation.prototype.valuation.enums.SubjectStatus;

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
@Table(name = "subject")
public class Subject {

    @Id
    private UUID id;

    @Column(name = "subject_code", nullable = false)
    private String subjectCode;

    @Column(name = "subject_name")
    private String subjectName;

    @Column(nullable = false)
    private String course;

    @Column(nullable = false)
    private String semester;

    @Column(name = "qp_code", nullable = false, unique = true)
    private String qpCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubjectStatus status = SubjectStatus.OPEN;

    @Column(name = "max_marks", nullable = false)
    private java.math.BigDecimal maxMarks = java.math.BigDecimal.valueOf(100);

    /** Credit weight of this subject in the program's curriculum, e.g. 4.0. */
    private java.math.BigDecimal credits;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course courseRef;

    @ManyToOne
    @JoinColumn(name = "chief_id")
    private User chief;

    @ManyToOne
    @JoinColumn(name = "opened_by", nullable = false)
    private User openedBy;

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

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
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

    public SubjectStatus getStatus() {
        return status;
    }

    public void setStatus(SubjectStatus status) {
        this.status = status;
    }

    public java.math.BigDecimal getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(java.math.BigDecimal maxMarks) {
        this.maxMarks = maxMarks;
    }

    public java.math.BigDecimal getCredits() {
        return credits;
    }

    public void setCredits(java.math.BigDecimal credits) {
        this.credits = credits;
    }

    public Course getCourseRef() {
        return courseRef;
    }

    public void setCourseRef(Course courseRef) {
        this.courseRef = courseRef;
    }

    public User getChief() {
        return chief;
    }

    public void setChief(User chief) {
        this.chief = chief;
    }

    public User getOpenedBy() {
        return openedBy;
    }

    public void setOpenedBy(User openedBy) {
        this.openedBy = openedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
