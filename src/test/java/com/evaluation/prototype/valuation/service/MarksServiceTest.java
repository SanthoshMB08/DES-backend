package com.evaluation.prototype.valuation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.evaluation.prototype.academic.entity.AcademicCurriculum;
import com.evaluation.prototype.academic.entity.AcademicSubject;
import com.evaluation.prototype.academic.entity.Program;
import com.evaluation.prototype.academic.repository.AcademicSubjectRepository;
import com.evaluation.prototype.valuation.dto.MarksLookupResponse;
import com.evaluation.prototype.valuation.entity.Subject;
import com.evaluation.prototype.valuation.entity.ValuationScript;
import com.evaluation.prototype.valuation.enums.SubjectStatus;
import com.evaluation.prototype.valuation.enums.ValuationStatus;
import com.evaluation.prototype.valuation.repository.SubjectRepository;
import com.evaluation.prototype.valuation.repository.ValuationScriptRepository;

class MarksServiceTest {

    @Test
    void lookupUsesConfiguredCurriculumSubjectsAndPublishedMarks() {
        Program program = mock(Program.class);
        when(program.getCode()).thenReturn("BCA");
        AcademicCurriculum curriculum = mock(AcademicCurriculum.class);
        when(curriculum.getProgram()).thenReturn(program);

        AcademicSubject academicSubject = mock(AcademicSubject.class);
        when(academicSubject.getCurriculum()).thenReturn(curriculum);
        when(academicSubject.getQpCode()).thenReturn("BCA-601");
        when(academicSubject.getSubjectName()).thenReturn("Data Structures");

        Subject valuationSubject = mock(Subject.class);
        when(valuationSubject.getStatus()).thenReturn(SubjectStatus.PUBLISHED);
        ValuationScript script = mock(ValuationScript.class);
        when(script.getStatus()).thenReturn(ValuationStatus.REVIEWED);
        when(script.getTotalMarks()).thenReturn(new BigDecimal("72.50"));

        AcademicSubjectRepository academicSubjectRepository = mock(AcademicSubjectRepository.class);
        ValuationScriptRepository scriptRepository = mock(ValuationScriptRepository.class);
        SubjectRepository subjectRepository = mock(SubjectRepository.class);
        when(academicSubjectRepository.findForMarksLookup("BCA", "6")).thenReturn(List.of(academicSubject));
        when(subjectRepository.findByQpCode("BCA-601")).thenReturn(Optional.of(valuationSubject));
        when(scriptRepository.findFirstByCandidateIdIgnoreCaseAndQpCodeIgnoreCase("STU-001", "BCA-601"))
                .thenReturn(Optional.of(script));

        MarksService service = new MarksService(academicSubjectRepository, scriptRepository, subjectRepository);
        MarksLookupResponse result = service.lookup("STU-001", " BCA ", "6");

        assertEquals("BCA", result.course());
        assertEquals(1, result.subjects().size());
        assertEquals("Data Structures", result.subjects().get(0).subjectName());
        assertEquals(72.5, result.subjects().get(0).marks());
        assertTrue(result.subjects().get(0).finalized());
        verify(academicSubjectRepository).findForMarksLookup("BCA", "6");
    }

    @Test
    void lookupWithoutCourseUsesSemesterOnlyQuery() {
        AcademicSubjectRepository academicSubjectRepository = mock(AcademicSubjectRepository.class);
        ValuationScriptRepository scriptRepository = mock(ValuationScriptRepository.class);
        SubjectRepository subjectRepository = mock(SubjectRepository.class);
        when(academicSubjectRepository.findAllForMarksLookup("6")).thenReturn(List.of());

        MarksService service = new MarksService(academicSubjectRepository, scriptRepository, subjectRepository);
        MarksLookupResponse result = service.lookup("STU-001", " ", "6");

        assertEquals("6", result.semester());
        assertTrue(result.subjects().isEmpty());
        verify(academicSubjectRepository).findAllForMarksLookup("6");
        verify(academicSubjectRepository, never()).findForMarksLookup(null, "6");
    }
}
