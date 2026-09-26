package com.evaluation.prototype.valuation.service;

import java.util.List;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

import com.evaluation.prototype.academic.entity.AcademicSubject;
import com.evaluation.prototype.academic.repository.AcademicSubjectRepository;
import com.evaluation.prototype.valuation.dto.MarksLookupResponse;
import com.evaluation.prototype.valuation.entity.Subject;
import com.evaluation.prototype.valuation.entity.ValuationScript;
import com.evaluation.prototype.valuation.enums.SubjectStatus;
import com.evaluation.prototype.valuation.enums.ValuationStatus;
import com.evaluation.prototype.valuation.repository.SubjectRepository;
import com.evaluation.prototype.valuation.repository.ValuationScriptRepository;

/**
 * Public marks lookup: given a roll number + semester, returns subjects from the
 * configured academic curriculum, with published marks where they exist and 0 otherwise.
 * Marks are only visible after the RE has published them (SubjectStatus.PUBLISHED).
 */
@Service
public class MarksService {

    private final AcademicSubjectRepository academicSubjectRepository;
    private final ValuationScriptRepository scriptRepository;
    private final SubjectRepository subjectRepository;

    public MarksService(AcademicSubjectRepository academicSubjectRepository,
                        ValuationScriptRepository scriptRepository,
                        SubjectRepository subjectRepository) {
        this.academicSubjectRepository = academicSubjectRepository;
        this.scriptRepository = scriptRepository;
        this.subjectRepository = subjectRepository;
    }

    public MarksLookupResponse lookup(String rollNumber, String course, String semester) {
        String courseFilter = course == null || course.isBlank() ? null : course.trim();
        List<AcademicSubject> subjects = courseFilter == null
                ? academicSubjectRepository.findAllForMarksLookup(semester)
                : academicSubjectRepository.findForMarksLookup(courseFilter, semester);

        List<MarksLookupResponse.SubjectMark> marks = IntStream.range(0, subjects.size())
                .mapToObj(index -> {
                    AcademicSubject subject = subjects.get(index);
                    boolean published = isPublished(subject.getQpCode());
                    ValuationScript script = published ? findCandidateScript(rollNumber, subject.getQpCode()) : null;
                    boolean finalized = script != null && script.getStatus() == ValuationStatus.REVIEWED;
                    double value = finalized && script.getTotalMarks() != null ? script.getTotalMarks().doubleValue() : 0d;
                    return new MarksLookupResponse.SubjectMark(
                            index + 1, subject.getSubjectName(), subject.getQpCode(), value, finalized && published);
                })
                .toList();

        AcademicSubject firstSubject = subjects.isEmpty() ? null : subjects.get(0);
        String resolvedCourse = firstSubject == null
                ? courseFilter
                : firstSubject.getCurriculum() != null
                        ? firstSubject.getCurriculum().getProgram().getCode()
                        : firstSubject.getSyllabus().getProgram() != null
                                ? firstSubject.getSyllabus().getProgram().getCode()
                                : courseFilter;
        return new MarksLookupResponse(rollNumber, resolvedCourse, semester, marks);
    }

    /** Marks are only shown if the subject has been PUBLISHED by the RE. */
    private boolean isPublished(String qpCode) {
        return subjectRepository.findByQpCode(qpCode)
                .map(s -> s.getStatus() == SubjectStatus.PUBLISHED)
                .orElse(false);
    }

    private ValuationScript findCandidateScript(String rollNumber, String qpCode) {
        return scriptRepository.findFirstByCandidateIdIgnoreCaseAndQpCodeIgnoreCase(rollNumber, qpCode).orElse(null);
    }
}
