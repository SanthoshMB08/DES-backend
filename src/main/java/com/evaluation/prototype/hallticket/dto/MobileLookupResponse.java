package com.evaluation.prototype.hallticket.dto;

import java.util.List;

import com.evaluation.prototype.common.enums.StudentType;

public record MobileLookupResponse(
        boolean found,
        String registerNumber,
        String semester,
        String studentName,
        StudentType studentType,
        List<MobileExamEntry> exams) {

    public static MobileLookupResponse notFound(String registerNumber, String semester) {
        return new MobileLookupResponse(false, registerNumber, semester, null, null, List.of());
    }
}
