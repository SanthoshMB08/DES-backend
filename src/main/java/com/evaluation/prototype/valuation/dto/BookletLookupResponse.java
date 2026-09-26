package com.evaluation.prototype.valuation.dto;

import java.time.LocalDate;

/**
 * Everything the scanner screen needs to fill itself in from an answer-booklet
 * number alone, so nothing has to be keyed in by hand.
 */
public record BookletLookupResponse(
    String answerBookletId,
    String candidateId,
    String candidateName,
    String qpCode,
    String course,
    Integer semester,
    String subject,
    LocalDate examDate,
    String centerCode,
    boolean found
) {}
