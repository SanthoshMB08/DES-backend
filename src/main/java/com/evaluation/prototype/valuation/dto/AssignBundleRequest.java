package com.evaluation.prototype.valuation.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AssignBundleRequest(@NotNull UUID evaluatorId) {
}
