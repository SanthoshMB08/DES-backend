package com.evaluation.prototype.valuation.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.evaluation.prototype.valuation.dto.MarksLookupResponse;
import com.evaluation.prototype.valuation.service.MarksService;

/**
 * Public results lookup: no login required, matching a typical "check your marks" portal.
 * Enter a roll number + semester and get every configured academic subject for that semester,
 * with 0 for anything not yet finalized.
 */
@RestController
@RequestMapping("/api/marks")
public class MarksController {

    private final MarksService marksService;

    public MarksController(MarksService marksService) {
        this.marksService = marksService;
    }

    @GetMapping
    public MarksLookupResponse lookup(
            @RequestParam String rollNumber,
            @RequestParam String semester,
            @RequestParam(required = false) String course) {
        return marksService.lookup(rollNumber, course, semester);
    }
}
