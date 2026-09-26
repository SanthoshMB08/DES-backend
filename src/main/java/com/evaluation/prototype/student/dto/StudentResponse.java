package com.evaluation.prototype.student.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.evaluation.prototype.student.entity.Student;

public record StudentResponse(
        UUID id,
        String registerNumber,
        String studentName,
        UUID batchId,
        String batchLabel,
        LocalDate dateOfBirth,
        String category,
        String gender) {

    public static StudentResponse from(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getRegisterNumber(),
                student.getStudentName(),
                student.getBatch() == null ? null : student.getBatch().getId(),
                student.getBatch() == null ? null : student.getBatch().getLabel(),
                student.getDateOfBirth(),
                student.getCategory(),
                student.getGender());
    }
}
