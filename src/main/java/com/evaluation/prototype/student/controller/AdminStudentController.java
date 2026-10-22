package com.evaluation.prototype.student.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.evaluation.prototype.student.dto.StudentResponse;
import com.evaluation.prototype.student.dto.StudentUpsertRequest;
import com.evaluation.prototype.student.service.StudentService;

import jakarta.validation.Valid;

/**
 * Admin-facing student management and HOD Excel import support.
 */
@RestController
@RequestMapping("/api/admin/students")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStudentController {

    private final StudentService studentService;

    public AdminStudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping
    public StudentResponse upsert(@Valid @RequestBody StudentUpsertRequest request) {
        return studentService.upsert(request);
    }

    @GetMapping
    public List<StudentResponse> search(@RequestParam(required = false) String registerNumber) {
        return studentService.search(registerNumber == null ? "" : registerNumber);
    }

    @GetMapping("/batch/{batchId}")
    public List<StudentResponse> forBatch(@PathVariable UUID batchId) {
        return studentService.listForBatch(batchId);
    }
}
