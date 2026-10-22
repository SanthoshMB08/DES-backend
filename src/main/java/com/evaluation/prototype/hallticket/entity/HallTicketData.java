package com.evaluation.prototype.hallticket.entity;

import java.time.Instant;
import java.util.UUID;

import com.evaluation.prototype.booklet.entity.BookletRecord;
import com.evaluation.prototype.student.entity.Student;
import com.evaluation.prototype.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "hall_ticket_data", uniqueConstraints = @UniqueConstraint(
        name = "uk_hall_ticket_data_qp_usn_month",
        columnNames = { "qp_code", "usn", "exam_year_month" }))
public class HallTicketData {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "usn", nullable = false, length = 50)
    private String usn;

    @Column(name = "qp_code", nullable = false, length = 50)
    private String qpCode;

    @Column(name = "exam_year_month", nullable = false, length = 7)
    private String examYearMonth;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booklet_id", unique = true)
    private BookletRecord booklet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

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
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public String getUsn() { return usn; }
    public void setUsn(String usn) { this.usn = usn; }
    public String getQpCode() { return qpCode; }
    public void setQpCode(String qpCode) { this.qpCode = qpCode; }
    public String getExamYearMonth() { return examYearMonth; }
    public void setExamYearMonth(String examYearMonth) { this.examYearMonth = examYearMonth; }
    public BookletRecord getBooklet() { return booklet; }
    public void setBooklet(BookletRecord booklet) { this.booklet = booklet; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
}
