package com.evaluation.prototype.valuation.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;

import com.evaluation.prototype.academic.dto.MarkingSection;
import com.evaluation.prototype.common.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;

final class MarkingSchemeScorer {

    private MarkingSchemeScorer() {
    }

    static BigDecimal calculate(JsonNode questions, List<MarkingSection> scheme) {
        if (questions == null || !questions.isArray()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Submitted question marks must be an array");
        }

        Map<String, List<BigDecimal>> scoresBySection = new HashMap<>();
        Map<String, MarkingSection> sections = new HashMap<>();
        for (MarkingSection section : scheme) {
            sections.put(section.name(), section);
            scoresBySection.put(section.name(), new ArrayList<>());
        }

        Set<String> questionNumbers = new HashSet<>();
        int expectedQuestionCount = scheme.stream().mapToInt(MarkingSection::questionCount).sum();
        if (questions.size() != expectedQuestionCount) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Submitted questions do not match the configured marking scheme");
        }

        for (JsonNode question : questions) {
            String qno = question.path("qno").asText("");
            String sectionName = question.path("section").asText("");
            MarkingSection section = sections.get(sectionName);
            if (qno.isBlank() || !questionNumbers.add(qno) || section == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Submitted questions do not match the configured marking scheme");
            }

            BigDecimal questionMax = decimal(question.get("max"), "Question maximum marks are invalid");
            if (questionMax.compareTo(section.marksPerQuestion()) != 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Question maximum marks do not match the configured marking scheme");
            }
            JsonNode obtainedNode = question.get("obt");
            BigDecimal obtained = obtainedNode == null || obtainedNode.isNull()
                    || obtainedNode.asText().equals("NA") || obtainedNode.asText().equals("BLK")
                            ? BigDecimal.ZERO
                            : decimal(obtainedNode, "Submitted marks are invalid");
            if (obtained.signum() < 0 || obtained.compareTo(section.marksPerQuestion()) > 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Submitted marks are outside the allowed range for " + qno);
            }
            scoresBySection.get(sectionName).add(obtained);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (MarkingSection section : scheme) {
            List<BigDecimal> scores = scoresBySection.get(section.name());
            if (scores.size() != section.questionCount()) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Submitted questions do not match the configured marking scheme");
            }
            scores.sort((first, second) -> second.compareTo(first));
            int consideredCount = section.maxMarks()
                    .divide(section.marksPerQuestion(), 0, RoundingMode.CEILING)
                    .intValueExact();
            BigDecimal sectionTotal = scores.stream()
                    .limit(Math.min(consideredCount, scores.size()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .min(section.maxMarks());
            total = total.add(sectionTotal);
        }
        return total;
    }

    private static BigDecimal decimal(JsonNode value, String message) {
        if (value == null || !value.isNumber() && !value.isTextual()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
        try {
            return new BigDecimal(value.asText());
        } catch (NumberFormatException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
    }
}
