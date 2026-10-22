package com.evaluation.prototype.valuation.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.evaluation.prototype.booklet.entity.BookletRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "exam_marks")
public class ExamMarksRecord {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booklet_id", nullable = false, unique = true)
    private BookletRecord booklet;

    @Column(name = "usn", nullable = false, length = 50)
    private String usn;

    @Column(name = "question_marks", columnDefinition = "TEXT")
    private String questionMarks;

    @Column(name = "total_marks", precision = 7, scale = 2)
    private BigDecimal totalMarks;

    @Column(name = "revaluation_question_marks", columnDefinition = "TEXT")
    private String revaluationQuestionMarks;

    @Column(name = "revaluation_total_marks", precision = 7, scale = 2)
    private BigDecimal revaluationTotalMarks;

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
    void preUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public BookletRecord getBooklet() { return booklet; }
    public void setBooklet(BookletRecord booklet) { this.booklet = booklet; }
    public String getUsn() { return usn; }
    public void setUsn(String usn) { this.usn = usn; }
    public String getQuestionMarks() { return questionMarks; }
    public void setQuestionMarks(String questionMarks) { this.questionMarks = questionMarks; }
    public BigDecimal getTotalMarks() { return totalMarks; }
    public void setTotalMarks(BigDecimal totalMarks) { this.totalMarks = totalMarks; }
    public String getRevaluationQuestionMarks() { return revaluationQuestionMarks; }
    public void setRevaluationQuestionMarks(String revaluationQuestionMarks) { this.revaluationQuestionMarks = revaluationQuestionMarks; }
    public BigDecimal getRevaluationTotalMarks() { return revaluationTotalMarks; }
    public void setRevaluationTotalMarks(BigDecimal revaluationTotalMarks) { this.revaluationTotalMarks = revaluationTotalMarks; }
}
