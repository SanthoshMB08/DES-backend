package com.evaluation.prototype.valuation.entity;

import java.time.Instant;
import java.util.UUID;

import com.evaluation.prototype.academic.entity.Program;
import com.evaluation.prototype.academic.entity.Syllabus;
import com.evaluation.prototype.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * A Course (e.g. "BCA", semester "6") is the top-level grouping the RE
 * creates before opening any subjects. The RE appoints exactly one Board
 * Officer (BO) to a Course - that BO then oversees every Subject (QP code)
 * created within it.
 */
@Entity
@Table(name = "course")
public class Course {

    @Id
    private UUID id;

    @Column(name = "course_name", nullable = false)
    private String courseName;

    @Column(nullable = false)
    private String semester;

    @Column(name = "syllabus_year", nullable = false)
    private String syllabusYear;

    @ManyToOne
    @JoinColumn(name = "syllabus_id")
    private Syllabus syllabus;

    /**
     * Optional link to the academic Program master record (see the `academic`
     * package). Nullable so existing free-text courseName/semester keep working
     * even for RE's who haven't set up Programs yet - see Phase 2 notes.
     */
    @ManyToOne
    @JoinColumn(name = "program_id")
    private Program program;

    @ManyToOne
    @JoinColumn(name = "bo_id")
    private User bo;

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

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getSyllabusYear() {
        return syllabusYear;
    }

    public void setSyllabusYear(String syllabusYear) {
        this.syllabusYear = syllabusYear;
    }

    public Syllabus getSyllabus() { return syllabus; }
    public void setSyllabus(Syllabus syllabus) { this.syllabus = syllabus; }

    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
    }

    public User getBo() {
        return bo;
    }

    public void setBo(User bo) {
        this.bo = bo;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
