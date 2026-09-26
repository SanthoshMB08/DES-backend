package com.evaluation.prototype.academic.dto;

import java.util.UUID;

public record SyllabusResponse(UUID id, UUID programId, String programName, String name, String year) {}
