package com.evaluation.prototype.academic.entity;

import java.time.Instant;
import java.util.UUID;
import com.evaluation.prototype.user.entity.User;
import jakarta.persistence.*;

@Entity
@Table(name = "academic_syllabus")
public class Syllabus {
    @Id private UUID id;
    @ManyToOne @JoinColumn(name = "program_id") private Program program;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String year;
    @ManyToOne @JoinColumn(name = "created_by") private User createdBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void prePersist() { if (id == null) id = UUID.randomUUID(); Instant now=Instant.now(); createdAt=now; updatedAt=now; }
    @PreUpdate void preUpdate() { updatedAt=Instant.now(); }
    public UUID getId(){return id;} public Program getProgram(){return program;} public void setProgram(Program p){program=p;}
    public String getName(){return name;} public void setName(String v){name=v;} public String getYear(){return year;} public void setYear(String v){year=v;}
    public void setCreatedBy(User u){createdBy=u;}
}
