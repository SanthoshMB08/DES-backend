package com.evaluation.prototype.hod.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.evaluation.prototype.academic.repository.AcademicSubjectRepository;
import com.evaluation.prototype.audit.service.AuditService;
import com.evaluation.prototype.hallticket.repository.HallTicketDataRepository;
import com.evaluation.prototype.student.entity.Student;
import com.evaluation.prototype.student.repository.StudentRepository;
import com.evaluation.prototype.user.entity.User;
import com.evaluation.prototype.valuation.repository.SubjectRepository;

class ExamRegistrationImportServiceTest {

    @Test
    void countsNewAndExistingRegistrationsWithoutCreatingDuplicates() {
        Student student = mock(Student.class);
        User actor = mock(User.class);
        UUID studentId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(student.getId()).thenReturn(studentId);
        when(actor.getId()).thenReturn(actorId);
        StudentRepository students = mock(StudentRepository.class);
        AcademicSubjectRepository academicSubjects = mock(AcademicSubjectRepository.class);
        SubjectRepository subjects = mock(SubjectRepository.class);
        HallTicketDataRepository registrations = mock(HallTicketDataRepository.class);

        when(students.findByRegisterNumber("USN001")).thenReturn(Optional.of(student));
        when(academicSubjects.existsByQpCodeIgnoreCase("QP001")).thenReturn(true);
        when(registrations.insertIfAbsent(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(studentId),
                org.mockito.ArgumentMatchers.eq("USN001"),
                org.mockito.ArgumentMatchers.eq("QP001"),
                org.mockito.ArgumentMatchers.eq("2026-10"),
                org.mockito.ArgumentMatchers.eq(actorId)))
                .thenReturn(1, 0);

        ExamRegistrationImportService service = new ExamRegistrationImportService(
                students, academicSubjects, subjects, registrations, mock(AuditService.class));

        var response = service.importRows(List.of(
                new ExamRegistrationImportService.ExamRegistrationRow(2, "USN001", "QP001", "2026-10"),
                new ExamRegistrationImportService.ExamRegistrationRow(3, "USN001", "QP001", "2026-10")), actor);

        assertEquals(2, response.rowsProcessed());
        assertEquals(1, response.registrationsCreated());
        assertEquals(1, response.registrationsAlreadyPresent());
    }
}
