package com.evaluation.prototype.booklet.entity;

import java.time.Instant;
import java.util.UUID;

import com.evaluation.prototype.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "booklet_record")
public class BookletRecord {

    @Id
    private UUID id;

    @Column(name = "booklet_number", nullable = false, unique = true, length = 80)
    private String bookletNumber;

    @Column(name = "original_pdf_url", columnDefinition = "TEXT")
    private String originalPdfUrl;

    @Column(name = "corrected_pdf_url", columnDefinition = "TEXT")
    private String correctedPdfUrl;

    @Column(name = "revaluation_pdf_url", columnDefinition = "TEXT")
    private String revaluationPdfUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluator_id")
    private User evaluator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boe_id")
    private User boe;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getBookletNumber() { return bookletNumber; }
    public void setBookletNumber(String bookletNumber) { this.bookletNumber = bookletNumber; }
    public String getOriginalPdfUrl() { return originalPdfUrl; }
    public void setOriginalPdfUrl(String originalPdfUrl) { this.originalPdfUrl = originalPdfUrl; }
    public String getCorrectedPdfUrl() { return correctedPdfUrl; }
    public void setCorrectedPdfUrl(String correctedPdfUrl) { this.correctedPdfUrl = correctedPdfUrl; }
    public String getRevaluationPdfUrl() { return revaluationPdfUrl; }
    public void setRevaluationPdfUrl(String revaluationPdfUrl) { this.revaluationPdfUrl = revaluationPdfUrl; }
    public User getEvaluator() { return evaluator; }
    public void setEvaluator(User evaluator) { this.evaluator = evaluator; }
    public User getReviewer() { return reviewer; }
    public void setReviewer(User reviewer) { this.reviewer = reviewer; }
    public User getBoe() { return boe; }
    public void setBoe(User boe) { this.boe = boe; }
}
