package com.evaluation.prototype.hallticket.dto;

import java.util.List;

import com.evaluation.prototype.common.enums.StudentType;

public record MobileScanResponse(
        String resultType,   // NEW_HALL_TICKET | NEW_EXAM_ENTRY | ALREADY_RECORDED
        String registerNumber,
        String semester,
        String studentName,
        StudentType studentType,
        List<MobileExamEntry> exams) {
}
