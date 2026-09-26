package com.evaluation.prototype.valuation.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
@Table(name = "valuation_script")
public class ValuationScript {

    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "bundle_id")
    private ValuationBundle bundle;

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "answer_booklet_id", nullable = false, unique = true)
    private String answerBookletId;

    @Column(name = "candidate_id")
    private String candidateId;

    @Column(name = "candidate_name")
    private String candidateName;

    @Column(name = "qp_code")
    private String qpCode;

    private String subject;

    @Column(name = "exam_date")
    private LocalDate examDate;

    private String course;
    private String semester;

    @Column(name = "center_code")
    private String centerCode;

    @Column(name = "scanned_file_name")
    private String scannedFileName;

    @Column(name = "file_url", columnDefinition = "TEXT")
    private String fileUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ValuationStatus status = ValuationStatus.PENDING;

    @Column(name = "marks_json", columnDefinition = "TEXT")
    private String marksJson;

    @Column(name = "total_marks", precision = 6, scale = 2)
    private BigDecimal totalMarks;

    @Column(name = "review_comments")
    private String reviewComments;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
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

    public ValuationBundle getBundle() {
        return bundle;
    }

    public void setBundle(ValuationBundle bundle) {
        this.bundle = bundle;
    }

    public String getAnswerBookletId() {
        return answerBookletId;
    }

    public void setAnswerBookletId(String answerBookletId) {
        this.answerBookletId = answerBookletId;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
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

    public LocalDate getExamDate() {
        return examDate;
    }

    public void setExamDate(LocalDate examDate) {
        this.examDate = examDate;
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

    public String getCenterCode() {
        return centerCode;
    }

    public void setCenterCode(String centerCode) {
        this.centerCode = centerCode;
    }

    public String getScannedFileName() {
        return scannedFileName;
    }

    public void setScannedFileName(String scannedFileName) {
        this.scannedFileName = scannedFileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public ValuationStatus getStatus() {
        return status;
    }

    public void setStatus(ValuationStatus status) {
        this.status = status;
    }

    public String getMarksJson() {
        return marksJson;
    }

    public void setMarksJson(String marksJson) {
        this.marksJson = marksJson;
    }

    public BigDecimal getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(BigDecimal totalMarks) {
        this.totalMarks = totalMarks;
    }

    public String getReviewComments() {
        return reviewComments;
    }

    public void setReviewComments(String reviewComments) {
        this.reviewComments = reviewComments;
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
