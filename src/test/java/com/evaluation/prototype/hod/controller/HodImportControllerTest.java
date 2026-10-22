package com.evaluation.prototype.hod.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.evaluation.prototype.academic.repository.BatchRepository;
import com.evaluation.prototype.auth.service.CurrentUserService;
import com.evaluation.prototype.common.exception.ApiException;
import com.evaluation.prototype.common.imports.ExcelTableReader;
import com.evaluation.prototype.hod.service.ExamRegistrationImportService;
import com.evaluation.prototype.student.service.StudentService;

class HodImportControllerTest {

    private final HodImportController controller = new HodImportController(
            new ExcelTableReader(),
            mock(BatchRepository.class),
            mock(StudentService.class),
            mock(ExamRegistrationImportService.class),
            mock(CurrentUserService.class));

    @Test
    void reportsWhichExamRegistrationValuesAreMissing() throws IOException {
        ApiException exception = assertThrows(ApiException.class,
                () -> controller.importExamRegistrations(workbook(" ", " ", "2026-10")));

        assertEquals("Excel row 2: USN/register number, QP code required.", exception.getMessage());
    }

    @Test
    void explainsInvalidExamYearMonthFormat() throws IOException {
        ApiException exception = assertThrows(ApiException.class,
                () -> controller.importExamRegistrations(workbook("1AB23CS001", "CS501", "October 2026")));

        assertEquals("Excel row 2: Year-month 'October 2026' is invalid. Use YYYY-MM (for example, 2026-10).",
                exception.getMessage());
    }

    private MockMultipartFile workbook(String usn, String qpCode, String yearMonth) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Exam registrations");
            var headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("USN");
            headerRow.createCell(1).setCellValue("qpCode");
            headerRow.createCell(2).setCellValue("yearMonth");
            var dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue(usn);
            dataRow.createCell(1).setCellValue(qpCode);
            dataRow.createCell(2).setCellValue(yearMonth);
            workbook.write(output);
            return new MockMultipartFile(
                    "file", "exam-registrations.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray());
        }
    }
}
