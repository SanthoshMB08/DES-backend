package com.evaluation.prototype.academic.entity;

import java.time.Instant;
import java.util.UUID;
import com.evaluation.prototype.user.entity.User;
import jakarta.persistence.*;

@Entity
@Table(name = "academic_curriculum")
public class AcademicCurriculum {
    @Id private UUID id;
    @ManyToOne(optional = false) @JoinColumn(name = "program_id") private Program program;
    @ManyToOne(optional = false) @JoinColumn(name = "syllabus_id") private Syllabus syllabus;
    @Column(nullable = false) private String semester;
    @ManyToOne @JoinColumn(name = "created_by") private User createdBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void prePersist() { if (id == null) id = UUID.randomUUID(); Instant n = Instant.now(); createdAt = n; updatedAt = n; }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public Program getProgram() { return program; }
    public void setProgram(Program value) { program = value; }
    public Syllabus getSyllabus() { return syllabus; }
    public void setSyllabus(Syllabus value) { syllabus = value; }
    public String getSemester() { return semester; }
    public void setSemester(String value) { semester = value; }
    public void setCreatedBy(User value) { createdBy = value; }
}
