package com.evaluation.prototype.valuation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.evaluation.prototype.attendance.repository.AttendanceRepository;
import com.evaluation.prototype.auth.service.CurrentUserService;
import com.evaluation.prototype.hallticket.repository.HallTicketExamRepository;
import com.evaluation.prototype.storage.FileStorageService;
import com.evaluation.prototype.user.entity.User;
import com.evaluation.prototype.user.repository.UserRepository;
import com.evaluation.prototype.valuation.dto.ScriptSubmitRequest;
import com.evaluation.prototype.valuation.entity.ValuationBundle;
import com.evaluation.prototype.valuation.entity.ValuationScript;
import com.evaluation.prototype.valuation.enums.ValuationStatus;
import com.evaluation.prototype.valuation.repository.SubjectRepository;
import com.evaluation.prototype.valuation.repository.ValuationBundleRepository;
import com.evaluation.prototype.valuation.repository.ValuationScriptRepository;

class ValuationServiceTest {

    @Test
    void submittingPaperDoesNotFinalizeBundleOrRestoreEvaluatorAvailability() {
        UUID bundleId = UUID.randomUUID();
        UUID scriptId = UUID.randomUUID();
        UUID valuatorId = UUID.randomUUID();
        User valuator = mock(User.class);
        when(valuator.getId()).thenReturn(valuatorId);

        ValuationBundle bundle = mock(ValuationBundle.class);
        when(bundle.getValuator()).thenReturn(valuator);
        when(bundle.getStatus()).thenReturn(ValuationStatus.PENDING);

        ValuationScript script = mock(ValuationScript.class);
        when(script.getId()).thenReturn(scriptId);
        when(script.getBundle()).thenReturn(bundle);
        when(script.getSubjectId()).thenReturn(null);
        when(script.getStatus()).thenReturn(ValuationStatus.PENDING, ValuationStatus.VALUATED);

        ValuationBundleRepository bundleRepository = mock(ValuationBundleRepository.class);
        ValuationScriptRepository scriptRepository = mock(ValuationScriptRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(valuator));
        when(scriptRepository.findById(scriptId)).thenReturn(Optional.of(script));
        when(scriptRepository.save(script)).thenReturn(script);

        ValuationService service = new ValuationService(
                bundleRepository,
                scriptRepository,
                mock(SubjectRepository.class),
                userRepository,
                currentUserService,
                mock(FileStorageService.class),
                mock(AttendanceRepository.class),
                mock(HallTicketExamRepository.class));

        service.submitScriptMarks(scriptId, new ScriptSubmitRequest("[]", BigDecimal.ZERO));

        verify(bundle).setStatus(ValuationStatus.PENDING);
        verify(bundleRepository).save(bundle);
        verify(scriptRepository, never()).findByBundleId(any(UUID.class));
        verify(valuator, never()).setAvailable(true);
        verify(userRepository, never()).save(valuator);
    }

    @Test
    void submitBundleKeepsSubmittedScriptsAndUnbundlesPendingScripts() {
        UUID bundleId = UUID.randomUUID();
        UUID valuatorId = UUID.randomUUID();
        User valuator = mock(User.class);
        when(valuator.getId()).thenReturn(valuatorId);

        ValuationBundle bundle = mock(ValuationBundle.class);
        when(bundle.getValuator()).thenReturn(valuator);
        when(bundle.getSubjectId()).thenReturn(null);
        when(bundle.getId()).thenReturn(bundleId);
        when(bundle.getStatus()).thenReturn(ValuationStatus.PENDING);

        List<ValuationScript> allScripts = new ArrayList<>();
        List<ValuationScript> submittedScripts = new ArrayList<>();
        List<ValuationScript> pendingScripts = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            ValuationScript script = mock(ValuationScript.class);
            if (index < 5) {
                when(script.getStatus()).thenReturn(ValuationStatus.VALUATED);
                submittedScripts.add(script);
            } else {
                when(script.getStatus()).thenReturn(ValuationStatus.PENDING);
                pendingScripts.add(script);
            }
            allScripts.add(script);
        }

        ValuationBundleRepository bundleRepository = mock(ValuationBundleRepository.class);
        ValuationScriptRepository scriptRepository = mock(ValuationScriptRepository.class);
        SubjectRepository subjectRepository = mock(SubjectRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(valuator));
        when(bundleRepository.findById(bundleId)).thenReturn(Optional.of(bundle));
        when(scriptRepository.findByBundleId(bundleId)).thenReturn(allScripts, submittedScripts);

        ValuationService service = new ValuationService(
                bundleRepository,
                scriptRepository,
                subjectRepository,
                userRepository,
                currentUserService,
                mock(FileStorageService.class),
                mock(AttendanceRepository.class),
                mock(HallTicketExamRepository.class));

        int returned = service.submitBundle(bundleId);

        assertEquals(15, returned);
        verify(scriptRepository).saveAll(pendingScripts);
        pendingScripts.forEach(script -> verify(script).setBundle(null));
        submittedScripts.forEach(script -> verify(script, never()).setBundle(null));
        verify(bundle).setTotalScripts(5);
        verify(bundle).setStatus(ValuationStatus.VALUATED);
        verify(valuator).setAvailable(true);
        verify(userRepository).save(valuator);
        verify(bundleRepository).save(bundle);
    }

    @Test
    void submitBundleKeepsAllPapersWhenEveryPaperIsCompleted() {
        UUID bundleId = UUID.randomUUID();
        UUID valuatorId = UUID.randomUUID();
        User valuator = mock(User.class);
        when(valuator.getId()).thenReturn(valuatorId);

        ValuationBundle bundle = mock(ValuationBundle.class);
        when(bundle.getValuator()).thenReturn(valuator);
        when(bundle.getSubjectId()).thenReturn(null);
        when(bundle.getId()).thenReturn(bundleId);
        when(bundle.getStatus()).thenReturn(ValuationStatus.PENDING);

        List<ValuationScript> completedScripts = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            ValuationScript script = mock(ValuationScript.class);
            when(script.getStatus()).thenReturn(ValuationStatus.VALUATED);
            completedScripts.add(script);
        }

        ValuationBundleRepository bundleRepository = mock(ValuationBundleRepository.class);
        ValuationScriptRepository scriptRepository = mock(ValuationScriptRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(valuator));
        when(bundleRepository.findById(bundleId)).thenReturn(Optional.of(bundle));
        when(scriptRepository.findByBundleId(bundleId)).thenReturn(completedScripts, completedScripts);

        ValuationService service = new ValuationService(
                bundleRepository,
                scriptRepository,
                mock(SubjectRepository.class),
                userRepository,
                currentUserService,
                mock(FileStorageService.class),
                mock(AttendanceRepository.class),
                mock(HallTicketExamRepository.class));

        int returned = service.submitBundle(bundleId);

        assertEquals(0, returned);
        verify(scriptRepository).saveAll(List.of());
        completedScripts.forEach(script -> verify(script, never()).setBundle(null));
        verify(bundle).setTotalScripts(20);
        verify(bundle).setStatus(ValuationStatus.VALUATED);
        verify(valuator).setAvailable(true);
        verify(userRepository).save(valuator);
    }
}
