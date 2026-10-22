package com.evaluation.prototype.common.imports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.evaluation.prototype.common.exception.ApiException;

class ExcelTableReaderTest {

    private final ExcelTableReader reader = new ExcelTableReader();

    @Test
    void readsExcelHeadersAndDataRows() throws IOException {
        MockMultipartFile file = workbook(
                new String[] { "Register Number", "Student Name", "Date of Birth" },
                new String[] { "abc123", "Student One", "2005-02-03" });

        var rows = reader.read(file, Set.of("registernumber", "studentname"));

        assertEquals(1, rows.size());
        assertEquals(2, rows.get(0).rowNumber());
        assertEquals("abc123", rows.get(0).get("registernumber"));
        assertEquals("Student One", rows.get(0).get("studentname"));
        assertEquals("2005-02-03", rows.get(0).get("dateofbirth"));
    }

    @Test
    void acceptsUsnAndYearMonthExamRegistrationHeaders() throws IOException {
        MockMultipartFile file = workbook(
                new String[] { "USN", "QP Code", "Year-Month" },
                new String[] { "abc123", "QP001", "2026-10" });

        var rows = reader.read(file, Set.of("registernumber", "qpcode", "examyearmonth"));

        assertEquals("abc123", rows.get(0).get("registernumber"));
        assertEquals("QP001", rows.get(0).get("qpcode"));
        assertEquals("2026-10", rows.get(0).get("examyearmonth"));
    }

    @Test
    void rejectsMissingRequiredColumnsBeforeImport() throws IOException {
        MockMultipartFile file = workbook(
                new String[] { "Register Number" },
                new String[] { "abc123" });

        ApiException exception = assertThrows(ApiException.class,
                () -> reader.read(file, Set.of("registernumber", "studentname")));

        assertEquals("Missing required Excel columns: studentname", exception.getMessage());
    }

    private MockMultipartFile workbook(String[] headers, String[] values) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Import");
            var headerRow = sheet.createRow(0);
            for (int column = 0; column < headers.length; column++) {
                headerRow.createCell(column).setCellValue(headers[column]);
            }
            var dataRow = sheet.createRow(1);
            for (int column = 0; column < values.length; column++) {
                dataRow.createCell(column).setCellValue(values[column]);
            }
            workbook.write(output);
            return new MockMultipartFile(
                    "file", "import.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray());
        }
    }
}
