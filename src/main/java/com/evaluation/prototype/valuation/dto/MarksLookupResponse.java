package com.evaluation.prototype.valuation.dto;

import java.util.List;

public record MarksLookupResponse(
        String rollNumber,
        String course,
        String semester,
        List<SubjectMark> subjects) {

    public record SubjectMark(int subjectOrder, String subjectName, String qpCode, double marks, boolean finalized) {
    }
}
