package com.evaluation.prototype.booklet.entity;

import java.time.Instant;
import java.util.UUID;

import com.evaluation.prototype.attendance.entity.Attendance;
import com.evaluation.prototype.common.enums.AnswerBookletStatus;
import com.evaluation.prototype.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "answer_booklet")
public class AnswerBooklet {

    @Id
    private UUID id;

    @Column(name = "booklet_number", nullable = false, unique = true)
    private String bookletNumber;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id", unique = true)
    private Attendance attendance;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnswerBookletStatus status;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getBookletNumber() {
        return bookletNumber;
    }

    public void setBookletNumber(String bookletNumber) {
        this.bookletNumber = bookletNumber;
    }

    public void assignTo(Attendance attendance, User assignedBy) {
        this.attendance = attendance;
        this.assignedBy = assignedBy;
        this.assignedAt = Instant.now();
        this.status = AnswerBookletStatus.ASSIGNED;
    }

    public void setStatus(AnswerBookletStatus status) {
        this.status = status;
    }
}
