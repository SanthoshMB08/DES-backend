package com.evaluation.prototype.student.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.evaluation.prototype.academic.repository.BatchRepository;
import com.evaluation.prototype.attendance.repository.AttendanceRepository;
import com.evaluation.prototype.common.exception.ApiException;
import com.evaluation.prototype.student.dto.StudentResponse;
import com.evaluation.prototype.student.dto.StudentUpsertRequest;
import com.evaluation.prototype.student.entity.Student;
import com.evaluation.prototype.student.repository.StudentRepository;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final BatchRepository batchRepository;

    public StudentService(
            StudentRepository studentRepository,
            AttendanceRepository attendanceRepository,
            BatchRepository batchRepository) {
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.batchRepository = batchRepository;
    }

    @Transactional
    public Student findOrCreateAndLinkAttendance(String registerNumber, String studentName) {
        String normalizedRegisterNumber = normalize(registerNumber);
        Student student = studentRepository.findByRegisterNumber(normalizedRegisterNumber)
                .map(existing -> updateNameIfUseful(existing, studentName))
                .orElseGet(() -> createStudent(normalizedRegisterNumber, studentName));

        attendanceRepository.findByRegisterNumberAndStudentIsNull(normalizedRegisterNumber).forEach(attendance -> {
            attendance.setStudent(student);
            attendanceRepository.save(attendance);
        });

        return student;
    }

    /**
     * Admin/exam-cell facing: pre-load a student before any exam touches them, or
     * fill in batch/DOB/category/gender on one an invigilator's scan already
     * created bare-bones. Finds-or-creates by register number, same pattern as
     * findOrCreateAndLinkAttendance above.
     */
    @Transactional
    public StudentResponse upsert(StudentUpsertRequest request) {
        String registerNumber = normalize(request.registerNumber());
        Student student = studentRepository.findByRegisterNumber(registerNumber).orElseGet(() -> {
            Student s = new Student();
            s.setRegisterNumber(registerNumber);
            return s;
        });
        if (StringUtils.hasText(request.studentName())) {
            student.setStudentName(request.studentName().trim());
        }
        if (request.batchId() != null) {
            student.setBatch(batchRepository.findById(request.batchId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Batch not found")));
        }
        if (request.dateOfBirth() != null) {
            student.setDateOfBirth(request.dateOfBirth());
        }
        if (StringUtils.hasText(request.category())) {
            student.setCategory(request.category().trim());
        }
        if (StringUtils.hasText(request.gender())) {
            student.setGender(request.gender().trim());
        }
        return StudentResponse.from(studentRepository.save(student));
    }

    public List<StudentResponse> search(String registerNumber) {
        return studentRepository.findByRegisterNumberContainingIgnoreCase(normalize(registerNumber)).stream()
                .map(StudentResponse::from)
                .toList();
    }

    public List<StudentResponse> listForBatch(UUID batchId) {
        return studentRepository.findByBatchIdOrderByRegisterNumberAsc(batchId).stream()
                .map(StudentResponse::from)
                .toList();
    }

    public String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private Student updateNameIfUseful(Student student, String studentName) {
        if (StringUtils.hasText(studentName) && !studentName.equals(student.getStudentName())) {
            student.setStudentName(studentName.trim());
            return studentRepository.save(student);
        }
        return student;
    }

    private Student createStudent(String registerNumber, String studentName) {
        Student student = new Student();
        student.setRegisterNumber(registerNumber);
        student.setStudentName(StringUtils.hasText(studentName) ? studentName.trim() : null);
        return studentRepository.save(student);
    }
}
