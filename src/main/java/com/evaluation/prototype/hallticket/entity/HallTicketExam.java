package com.evaluation.prototype.hallticket.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "hall_ticket_exam",
        uniqueConstraints = @UniqueConstraint(name = "uk_hall_ticket_exam",
                columnNames = {"hall_ticket_id", "exam_date", "qp_code"}))
public class HallTicketExam {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hall_ticket_id", nullable = false)
    private HallTicket hallTicket;

    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @Column(name = "qp_code", nullable = false)
    private String qpCode;

    @Column(name = "course_name")
    private String courseName;

    @Column(name = "exam_time")
    private String examTime;

    @Column(name = "centre_code")
    private String centreCode;

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

    public HallTicket getHallTicket() {
        return hallTicket;
    }

    public void setHallTicket(HallTicket hallTicket) {
        this.hallTicket = hallTicket;
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

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getExamTime() {
        return examTime;
    }

    public void setExamTime(String examTime) {
        this.examTime = examTime;
    }

    public String getCentreCode() {
        return centreCode;
    }

    public void setCentreCode(String centreCode) {
        this.centreCode = centreCode;
    }
}
