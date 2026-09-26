package com.evaluation.prototype.valuation.dto;

import java.util.UUID;

// The BO's "status of every paper, per subject" rollup (GET /api/bo/subjects) is
// just a list of these - one row per subject in the BO's course.
public record SubjectProgressResponse(
    UUID subjectId,
    String subjectCode,
    String subjectName,
    String course,
    String semester,
    String qpCode,
    String status,
    String chiefName,
    UUID chiefId,
    int totalScripts,
    int valuatedScripts,
    int reviewedScripts
) {}
