package com.evaluation.prototype.hallticket.dto;

import java.util.List;

public record HallTicketImportResponse(
        int studentsProcessed,
        int hallTicketsCreated,
        int examRowsCreated,
        int examRowsUpdated,
        List<String> warnings) {
}
