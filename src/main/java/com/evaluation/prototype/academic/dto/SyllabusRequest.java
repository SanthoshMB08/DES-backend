package com.evaluation.prototype.academic.dto;

import jakarta.validation.constraints.NotBlank;

public record SyllabusRequest(@NotBlank String name, @NotBlank String year) {}
