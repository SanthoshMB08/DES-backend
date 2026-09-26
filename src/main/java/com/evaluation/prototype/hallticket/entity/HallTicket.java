package com.evaluation.prototype.hallticket.entity;

import java.time.Instant;
import java.util.UUID;

import com.evaluation.prototype.common.enums.StudentType;
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
import jakarta.persistence.Table;

@Entity
@Table(name = "hall_ticket")
public class HallTicket {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "register_number", nullable = false)
    private String registerNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "student_type", nullable = false)
    private StudentType studentType;

    @Column(nullable = false)
    private String semester;

    @Column(name = "qr_hash", unique = true)
    private String qrHash;

    @Column(name = "scanned_at", nullable = false)
    private Instant scannedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scanned_by")
    private User scannedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        createdAt = now;
        if (scannedAt == null) {
            scannedAt = now;
        }
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

    public StudentType getStudentType() {
        return studentType;
    }

    public void setStudentType(StudentType studentType) {
        this.studentType = studentType;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public Instant getScannedAt() {
        return scannedAt;
    }

    public User getScannedBy() {
        return scannedBy;
    }

    public String getQrHash() {
        return qrHash;
    }

    public void setQrHash(String qrHash) {
        this.qrHash = qrHash;
    }

    public void setScannedBy(User scannedBy) {
        this.scannedBy = scannedBy;
    }
}
