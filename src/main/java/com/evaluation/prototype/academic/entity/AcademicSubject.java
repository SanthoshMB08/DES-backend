package com.evaluation.prototype.academic.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.evaluation.prototype.user.entity.User;
import jakarta.persistence.*;

@Entity
@Table(name = "academic_subject")
public class AcademicSubject {
    @Id private UUID id;
    @ManyToOne(optional=false) @JoinColumn(name="syllabus_id") private Syllabus syllabus;
    @ManyToOne @JoinColumn(name="curriculum_id") private AcademicCurriculum curriculum;
    @Column(nullable=false) private String semester;
    @Column(name="subject_code", nullable=false) private String subjectCode;
    @Column(name="subject_name", nullable=false) private String subjectName;
    @Column(name="qp_code", nullable=false) private String qpCode;
    @Column(name="max_marks", nullable=false) private BigDecimal maxMarks = BigDecimal.valueOf(100);
    private BigDecimal credits;
    @ManyToOne @JoinColumn(name="created_by") private User createdBy;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    @PrePersist void prePersist(){if(id==null)id=UUID.randomUUID(); Instant n=Instant.now();createdAt=n;updatedAt=n;}
    @PreUpdate void preUpdate(){updatedAt=Instant.now();}
    public UUID getId(){return id;} public Syllabus getSyllabus(){return syllabus;} public void setSyllabus(Syllabus v){syllabus=v;}
    public AcademicCurriculum getCurriculum(){return curriculum;} public void setCurriculum(AcademicCurriculum v){curriculum=v;}
    public String getSemester(){return semester;} public void setSemester(String v){semester=v;} public String getSubjectCode(){return subjectCode;} public void setSubjectCode(String v){subjectCode=v;}
    public String getSubjectName(){return subjectName;} public void setSubjectName(String v){subjectName=v;} public String getQpCode(){return qpCode;} public void setQpCode(String v){qpCode=v;}
    public BigDecimal getMaxMarks(){return maxMarks;} public void setMaxMarks(BigDecimal v){maxMarks=v;} public BigDecimal getCredits(){return credits;} public void setCredits(BigDecimal v){credits=v;}
    public void setCreatedBy(User v){createdBy=v;}
}
