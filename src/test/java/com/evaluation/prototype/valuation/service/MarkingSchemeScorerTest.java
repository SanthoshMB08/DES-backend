package com.evaluation.prototype.valuation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.evaluation.prototype.academic.dto.MarkingSection;
import com.evaluation.prototype.common.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;

class MarkingSchemeScorerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void selectsTopFiveMarksFromSixTwoMarkQuestions() throws Exception {
        var questions = objectMapper.readTree("""
                [
                  {"qno":"Q1","section":"Section A","max":2,"obt":"2"},
                  {"qno":"Q2","section":"Section A","max":2,"obt":"2"},
                  {"qno":"Q3","section":"Section A","max":2,"obt":"2"},
                  {"qno":"Q4","section":"Section A","max":2,"obt":"2"},
                  {"qno":"Q5","section":"Section A","max":2,"obt":"2"},
                  {"qno":"Q6","section":"Section A","max":2,"obt":"2"}
                ]
                """);
        var scheme = List.of(new MarkingSection(
                "Section A", new BigDecimal("10"), new BigDecimal("2"), 6));

        assertEquals(new BigDecimal("10"), MarkingSchemeScorer.calculate(questions, scheme));
    }

    @Test
    void selectsTopSixMarksFromEightFiveMarkQuestions() throws Exception {
        var questions = objectMapper.readTree("""
                [
                  {"qno":"Q1","section":"Section B","max":5,"obt":"5"},
                  {"qno":"Q2","section":"Section B","max":5,"obt":"5"},
                  {"qno":"Q3","section":"Section B","max":5,"obt":"5"},
                  {"qno":"Q4","section":"Section B","max":5,"obt":"5"},
                  {"qno":"Q5","section":"Section B","max":5,"obt":"5"},
                  {"qno":"Q6","section":"Section B","max":5,"obt":"5"},
                  {"qno":"Q7","section":"Section B","max":5,"obt":"5"},
                  {"qno":"Q8","section":"Section B","max":5,"obt":"5"}
                ]
                """);
        var scheme = List.of(new MarkingSection(
                "Section B", new BigDecimal("30"), new BigDecimal("5"), 8));

        assertEquals(new BigDecimal("30"), MarkingSchemeScorer.calculate(questions, scheme));
    }

    @Test
    void selectsHighestScoringQuestionsInsteadOfFirstQuestions() throws Exception {
        var questions = objectMapper.readTree("""
                [
                  {"qno":"Q1","section":"Section A","max":2,"obt":"0"},
                  {"qno":"Q2","section":"Section A","max":2,"obt":"0"},
                  {"qno":"Q3","section":"Section A","max":2,"obt":"2"},
                  {"qno":"Q4","section":"Section A","max":2,"obt":"2"},
                  {"qno":"Q5","section":"Section A","max":2,"obt":"2"},
                  {"qno":"Q6","section":"Section A","max":2,"obt":"2"}
                ]
                """);
        var scheme = List.of(new MarkingSection(
                "Section A", new BigDecimal("10"), new BigDecimal("2"), 6));

        assertEquals(new BigDecimal("8"), MarkingSchemeScorer.calculate(questions, scheme));
    }

    @Test
    void rejectsMarksThatDoNotMatchConfiguredQuestionCount() throws Exception {
        var questions = objectMapper.readTree("""
                [{"qno":"Q1","section":"Section A","max":2,"obt":"2"}]
                """);
        var scheme = List.of(new MarkingSection(
                "Section A", new BigDecimal("10"), new BigDecimal("2"), 6));

        assertThrows(ApiException.class, () -> MarkingSchemeScorer.calculate(questions, scheme));
    }
}
