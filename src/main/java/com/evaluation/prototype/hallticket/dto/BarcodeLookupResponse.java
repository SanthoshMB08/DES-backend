package com.evaluation.prototype.hallticket.dto;

import java.util.List;

public record BarcodeLookupResponse(
        boolean found,
        String registerNumber,
        String studentName,
        // Usually exactly one entry (today's exam for this student). Empty means
        // "no exam scheduled for this student today" - zero pre-loaded data for
        // this student/date, or they're not writing today. More than one means
        // the student has two exams today and the invigilator needs to pick.
        List<BarcodeExamMatch> matches) {

    public static BarcodeLookupResponse notFound(String registerNumber) {
        return new BarcodeLookupResponse(false, registerNumber, null, List.of());
    }
}
