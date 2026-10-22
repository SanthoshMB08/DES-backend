package com.evaluation.prototype.valuation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.evaluation.prototype.auth.service.CurrentUserService;
import com.evaluation.prototype.common.exception.ApiException;
import com.evaluation.prototype.user.entity.User;
import com.evaluation.prototype.valuation.entity.Course;
import com.evaluation.prototype.valuation.entity.Subject;
import com.evaluation.prototype.valuation.entity.ValuationScript;
import com.evaluation.prototype.valuation.enums.ValuationStatus;
import com.evaluation.prototype.valuation.repository.SubjectRepository;
import com.evaluation.prototype.valuation.repository.ValuationScriptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BoeReportServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private ValuationScriptRepository scriptRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private BoeReportService service;

    @Test
    void reportsPaperCountsAndRoleNumberMarksForTheAssignedCourse() {
        UUID subjectId = UUID.randomUUID();
        UUID boId = UUID.randomUUID();
        User bo = user(boId);
        Course course = new Course();
        course.setBo(bo);
        Subject subject = subject(subjectId, course);
        ValuationScript marked = script("R001", "BOOKLET-1", BigDecimal.valueOf(72), ValuationStatus.REVIEWED);
        ValuationScript pending = script("R002", "BOOKLET-2", null, ValuationStatus.PENDING);
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(bo));
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));
        when(scriptRepository.findBySubjectId(subjectId)).thenReturn(List.of(marked, pending));

        var report = service.getSubjectReport(subjectId);

        assertEquals(2, report.totalPapers());
        assertEquals(1, report.markedPapers());
        assertEquals("R001", report.results().get(0).roleNumber());
        assertEquals(BigDecimal.valueOf(72), report.results().get(0).marks());
        assertEquals("R002", report.results().get(1).roleNumber());
    }

    @Test
    void rejectsSubjectsOutsideTheBoardsCourse() {
        UUID subjectId = UUID.randomUUID();
        Course course = new Course();
        course.setBo(user(UUID.randomUUID()));
        Subject subject = subject(subjectId, course);
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(user(UUID.randomUUID())));
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));

        assertThrows(ApiException.class, () -> service.getSubjectReport(subjectId));
    }

    private User user(UUID id) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Subject subject(UUID id, Course course) {
        Subject subject = new Subject();
        ReflectionTestUtils.setField(subject, "id", id);
        subject.setSubjectCode("SUB001");
        subject.setSubjectName("Example Subject");
        subject.setQpCode("QP001");
        subject.setCourse("BCA");
        subject.setSemester("6");
        subject.setCourseRef(course);
        return subject;
    }

    private ValuationScript script(String candidateId, String bookletId, BigDecimal marks, ValuationStatus status) {
        ValuationScript script = new ValuationScript();
        script.setCandidateId(candidateId);
        script.setAnswerBookletId(bookletId);
        script.setTotalMarks(marks);
        script.setStatus(status);
        return script;
    }
}
