package com.evaluation.prototype.valuation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.evaluation.prototype.auth.service.CurrentUserService;
import com.evaluation.prototype.booklet.entity.BookletRecord;
import com.evaluation.prototype.booklet.repository.BookletRecordRepository;
import com.evaluation.prototype.common.exception.ApiException;
import com.evaluation.prototype.hallticket.entity.HallTicketData;
import com.evaluation.prototype.hallticket.repository.HallTicketDataRepository;
import com.evaluation.prototype.student.entity.Student;
import com.evaluation.prototype.storage.SupabaseStorageService;
import com.evaluation.prototype.user.entity.User;
import com.evaluation.prototype.valuation.entity.ValuationScript;
import com.evaluation.prototype.valuation.repository.SubjectRepository;
import com.evaluation.prototype.valuation.repository.ValuationScriptRepository;

class ScannerBookletServiceTest {

    @Test
    void submissionCreatesValuationScriptStoresPdfAndLinksBookletToRegistration() {
        UUID registrationId = UUID.randomUUID();
        UUID scriptId = UUID.randomUUID();
        UUID bookletId = UUID.randomUUID();
        HallTicketData registration = mock(HallTicketData.class);
        Student student = mock(Student.class);
        User scanner = mock(User.class);
        HallTicketDataRepository registrationRepository = mock(HallTicketDataRepository.class);
        BookletRecordRepository bookletRepository = mock(BookletRecordRepository.class);
        ValuationScriptRepository scriptRepository = mock(ValuationScriptRepository.class);
        SubjectRepository subjectRepository = mock(SubjectRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        SupabaseStorageService storageService = mock(SupabaseStorageService.class);

        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(registration.getBooklet()).thenReturn(null);
        when(registration.getUsn()).thenReturn("1AB23CS001");
        when(registration.getQpCode()).thenReturn("BCA0105");
        when(registration.getExamYearMonth()).thenReturn("2024-09");
        when(registration.getStudent()).thenReturn(student);
        when(student.getStudentName()).thenReturn("Test Student");
        when(student.getCenterCode()).thenReturn("CENTER-1");
        when(bookletRepository.findByBookletNumberIgnoreCase("BOOKLET-1")).thenReturn(Optional.empty());
        when(scriptRepository.findByAnswerBookletId("BOOKLET-1")).thenReturn(Optional.empty());
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(scanner));
        when(subjectRepository.findByQpCodeIgnoreCase("BCA0105")).thenReturn(Optional.empty());
        when(scriptRepository.saveAndFlush(any(ValuationScript.class))).thenAnswer(invocation -> {
            ValuationScript script = invocation.getArgument(0);
            ReflectionTestUtils.setField(script, "id", scriptId);
            return script;
        });
        when(bookletRepository.saveAndFlush(any(BookletRecord.class))).thenAnswer(invocation -> {
            BookletRecord booklet = invocation.getArgument(0);
            ReflectionTestUtils.setField(booklet, "id", bookletId);
            return booklet;
        });

        ScannerBookletService service = new ScannerBookletService(
                registrationRepository,
                bookletRepository,
                scriptRepository,
                subjectRepository,
                currentUserService,
                storageService);

        var response = service.submit(registrationId, "booklet-1", mock(org.springframework.web.multipart.MultipartFile.class));

        assertEquals(scriptId, response.scriptId());
        assertEquals(bookletId, response.bookletRecordId());
        assertEquals("BOOKLET-1", response.bookletNumber());
        assertEquals("BCA0105/qp-original/" + scriptId + ".pdf", response.originalPdfPath());
        verify(registration).setBooklet(any(BookletRecord.class));
        verify(storageService).storeOriginal(
                eq(scriptId), eq("BCA0105"), any(org.springframework.web.multipart.MultipartFile.class));
    }

    @Test
    void submissionRejectsRegistrationThatAlreadyHasBooklet() {
        UUID registrationId = UUID.randomUUID();
        HallTicketData registration = mock(HallTicketData.class);
        HallTicketDataRepository registrationRepository = mock(HallTicketDataRepository.class);
        when(registrationRepository.findById(registrationId)).thenReturn(Optional.of(registration));
        when(registration.getBooklet()).thenReturn(mock(BookletRecord.class));

        BookletRecordRepository bookletRepository = mock(BookletRecordRepository.class);
        ValuationScriptRepository scriptRepository = mock(ValuationScriptRepository.class);
        SupabaseStorageService storageService = mock(SupabaseStorageService.class);
        ScannerBookletService service = new ScannerBookletService(
                registrationRepository,
                bookletRepository,
                scriptRepository,
                mock(SubjectRepository.class),
                mock(CurrentUserService.class),
                storageService);

        assertThrows(ApiException.class, () -> service.submit(
                registrationId, "BOOKLET-2", mock(org.springframework.web.multipart.MultipartFile.class)));
        verify(storageService, never()).storeOriginal(any(), any(), any());
    }

}
