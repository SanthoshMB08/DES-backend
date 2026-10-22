package com.evaluation.prototype.valuation.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.evaluation.prototype.auth.service.CurrentUserService;
import com.evaluation.prototype.common.exception.ApiException;
import com.evaluation.prototype.user.entity.User;
import com.evaluation.prototype.valuation.dto.BoeSubjectReportResponse;
import com.evaluation.prototype.valuation.entity.Subject;
import com.evaluation.prototype.valuation.entity.ValuationScript;
import com.evaluation.prototype.valuation.repository.SubjectRepository;
import com.evaluation.prototype.valuation.repository.ValuationScriptRepository;

@Service
public class BoeReportService {

    private static final Logger log = LoggerFactory.getLogger(BoeReportService.class);

    private final SubjectRepository subjectRepository;
    private final ValuationScriptRepository scriptRepository;
    private final CurrentUserService currentUserService;

    public BoeReportService(
            SubjectRepository subjectRepository,
            ValuationScriptRepository scriptRepository,
            CurrentUserService currentUserService) {
        this.subjectRepository = subjectRepository;
        this.scriptRepository = scriptRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public BoeSubjectReportResponse getSubjectReport(UUID subjectId) {
        User user = currentUserService.getCurrentUser()
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Subject not found"));

        boolean isAdmin = user.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
        if (!isAdmin && (subject.getCourseRef() == null || subject.getCourseRef().getBo() == null
                || !subject.getCourseRef().getBo().getId().equals(user.getId()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This subject is not in your course");
        }

        List<ValuationScript> scripts = scriptRepository.findBySubjectId(subjectId);
        List<BoeSubjectReportResponse.ResultRow> results = scripts.stream()
                .map(script -> new BoeSubjectReportResponse.ResultRow(
                        script.getCandidateId() == null || script.getCandidateId().isBlank()
                                ? script.getAnswerBookletId()
                                : script.getCandidateId(),
                        script.getTotalMarks(),
                        script.getStatus().name()))
                .sorted(Comparator.comparing(BoeSubjectReportResponse.ResultRow::roleNumber,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();

        int markedPapers = (int) scripts.stream().filter(script -> script.getTotalMarks() != null).count();
        return new BoeSubjectReportResponse(
                subject.getId(),
                subject.getSubjectCode(),
                subject.getSubjectName(),
                subject.getQpCode(),
                subject.getCourse(),
                subject.getSemester(),
                scripts.size(),
                markedPapers,
                results);
    }

    @Transactional(readOnly = true)
    public byte[] exportSubjectExcel(UUID subjectId) {
        BoeSubjectReportResponse report = getSubjectReport(subjectId);
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Results");
            Row title = sheet.createRow(0);
            title.createCell(0).setCellValue(report.subjectName() + " (" + report.qpCode() + ")");
            Row summary = sheet.createRow(1);
            summary.createCell(0).setCellValue("Total papers");
            summary.createCell(1).setCellValue(report.totalPapers());
            summary.createCell(2).setCellValue("Marked");
            summary.createCell(3).setCellValue(report.markedPapers());

            Row header = sheet.createRow(3);
            header.createCell(0).setCellValue("Role Number");
            header.createCell(1).setCellValue("Marks");
            header.createCell(2).setCellValue("Status");
            int rowIndex = 4;
            for (BoeSubjectReportResponse.ResultRow result : report.results()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(result.roleNumber());
                if (result.marks() != null) {
                    row.createCell(1).setCellValue(result.marks().doubleValue());
                }
                row.createCell(2).setCellValue(result.status());
            }
            sheet.setColumnWidth(0, 24 * 256);
            sheet.setColumnWidth(1, 14 * 256);
            sheet.setColumnWidth(2, 18 * 256);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException e) {
            log.error("Could not create results workbook for subject {}", subjectId, e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate the Excel report");
        }
    }
}
