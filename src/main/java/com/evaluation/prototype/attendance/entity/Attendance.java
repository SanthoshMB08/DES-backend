package com.evaluation.prototype.attendance.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.evaluation.prototype.common.enums.AttendanceSource;
import com.evaluation.prototype.common.enums.AttendanceStatus;
import com.evaluation.prototype.student.entity.Student;
import com.evaluation.prototype.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "attendance",
        uniqueConstraints = @UniqueConstraint(name = "uk_attendance_exam",
                columnNames = {"register_number", "exam_date", "qp_code"}))
public class Attendance {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(name = "register_number", nullable = false)
    private String registerNumber;

    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @Column(name = "qp_code", nullable = false)
    private String qpCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;

    @Column(name = "answer_booklet_number", unique = true)
    private String answerBookletNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marked_by")
    private User markedBy;

    @Column(name = "marked_at")
    private Instant markedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceSource source;

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

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public String getRegisterNumber() {
        return registerNumber;
    }

    public void setRegisterNumber(String registerNumber) {
        this.registerNumber = registerNumber;
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public void setExamDate(LocalDate examDate) {
        this.examDate = examDate;
    }

    public String getQpCode() {
        return qpCode;
    }

    public void setQpCode(String qpCode) {
        this.qpCode = qpCode;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public String getAnswerBookletNumber() {
        return answerBookletNumber;
    }

    public void setAnswerBookletNumber(String answerBookletNumber) {
        this.answerBookletNumber = answerBookletNumber;
    }

    public void setMarkedBy(User markedBy) {
        this.markedBy = markedBy;
    }

    public void setMarkedAt(Instant markedAt) {
        this.markedAt = markedAt;
    }

    public void setSource(AttendanceSource source) {
        this.source = source;
    }
}
