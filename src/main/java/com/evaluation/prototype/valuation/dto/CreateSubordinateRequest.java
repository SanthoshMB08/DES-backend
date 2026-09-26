package com.evaluation.prototype.valuation.dto;

import jakarta.validation.constraints.NotBlank;

// Used by RE (creating a BO), BO (creating a Reviewer) and Reviewer (creating an Evaluator) -
// the same shape works for all three since it's just "create an account under me".
public record CreateSubordinateRequest(
        @NotBlank String username,
        @NotBlank String password,
        String fullName) {
}
