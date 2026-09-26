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

    // Represents the max SEE (Semester End Exam) marks - this evaluation app is
    // the SEE engine. See the CIE-side fields below, added for Phase 2.
    @Column(name = "max_marks", nullable = false)
    private java.math.BigDecimal maxMarks = java.math.BigDecimal.valueOf(100);

    /** Credit weight of this subject in the program's curriculum, e.g. 4.0. */
    private java.math.BigDecimal credits;

    /** What fraction of the final grade comes from CIE (internal) marks, e.g. 40.00 for 40%. */
    @Column(name = "cie_weightage")
    private java.math.BigDecimal cieWeightage;

    /** What fraction of the final grade comes from SEE (external) marks, e.g. 60.00 for 60%. */
    @Column(name = "see_weightage")
    private java.math.BigDecimal seeWeightage;

    @Column(name = "max_cie_marks")
    private java.math.BigDecimal maxCieMarks;

    /** Minimum combined (CIE + SEE) marks needed to pass this subject. */
    @Column(name = "min_pass_marks")
    private java.math.BigDecimal minPassMarks;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course courseRef;

    @ManyToOne
    @JoinColumn(name = "chief_id")
    private User chief;

    /** The subject's teaching faculty, assigned by the BO (parallel to Chief for SEE) - enters CIE marks. */
    @ManyToOne
    @JoinColumn(name = "faculty_id")
    private User faculty;

    /** Once locked, no further CIE marks can be entered or changed for this subject. */
    @Column(name = "cie_locked", nullable = false)
    private boolean cieLocked = false;

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

    public java.math.BigDecimal getCieWeightage() {
        return cieWeightage;
    }

    public void setCieWeightage(java.math.BigDecimal cieWeightage) {
        this.cieWeightage = cieWeightage;
    }

    public java.math.BigDecimal getSeeWeightage() {
        return seeWeightage;
    }

    public void setSeeWeightage(java.math.BigDecimal seeWeightage) {
        this.seeWeightage = seeWeightage;
    }

    public java.math.BigDecimal getMaxCieMarks() {
        return maxCieMarks;
    }

    public void setMaxCieMarks(java.math.BigDecimal maxCieMarks) {
        this.maxCieMarks = maxCieMarks;
    }

    public java.math.BigDecimal getMinPassMarks() {
        return minPassMarks;
    }

    public void setMinPassMarks(java.math.BigDecimal minPassMarks) {
        this.minPassMarks = minPassMarks;
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

    public User getFaculty() {
        return faculty;
    }

    public void setFaculty(User faculty) {
        this.faculty = faculty;
    }

    public boolean isCieLocked() {
        return cieLocked;
    }

    public void setCieLocked(boolean cieLocked) {
        this.cieLocked = cieLocked;
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
