package com.evaluation.prototype.academic.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AcademicCurriculumRequest(@NotNull UUID programId, @NotNull UUID syllabusId, @NotBlank String semester) {}
