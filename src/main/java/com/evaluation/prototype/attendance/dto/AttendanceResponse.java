package com.evaluation.prototype.attendance.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.evaluation.prototype.attendance.entity.Attendance;
import com.evaluation.prototype.common.enums.AttendanceStatus;

public record AttendanceResponse(
        UUID id,
        UUID studentId,
        String registerNumber,
        LocalDate examDate,
        String qpCode,
        AttendanceStatus status,
        String answerBookletNumber) {

    public static AttendanceResponse from(Attendance attendance) {
        return new AttendanceResponse(
                attendance.getId(),
                attendance.getStudent() == null ? null : attendance.getStudent().getId(),
                attendance.getRegisterNumber(),
                attendance.getExamDate(),
                attendance.getQpCode(),
                attendance.getStatus(),
                attendance.getAnswerBookletNumber());
    }
}
