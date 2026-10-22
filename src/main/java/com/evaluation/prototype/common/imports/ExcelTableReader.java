package com.evaluation.prototype.common.imports;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.evaluation.prototype.common.exception.ApiException;

@Component
public class ExcelTableReader {

    private static final long MAX_FILE_BYTES = 10 * 1024 * 1024;
    private static final int MAX_DATA_ROWS = 5_000;

    public List<ExcelRow> read(MultipartFile file, Set<String> requiredHeaders) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a non-empty Excel file.");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Excel files must be 10 MB or smaller.");
        }

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "The Excel workbook has no worksheets.");
            }
            var sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "The first worksheet is empty.");
            }

            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            var evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Map<Integer, String> headerByColumn = new HashMap<>();
            Set<String> headers = new HashSet<>();
            for (int column = 0; column < headerRow.getLastCellNum(); column++) {
                String header = normalizeHeader(formatter.formatCellValue(headerRow.getCell(column), evaluator));
                if (!header.isEmpty()) {
                    if (!headers.add(header)) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "The Excel file contains duplicate column headers.");
                    }
                    headerByColumn.put(column, header);
                }
            }
            if (!headers.containsAll(requiredHeaders)) {
                Set<String> missing = new HashSet<>(requiredHeaders);
                missing.removeAll(headers);
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Missing required Excel columns: " + String.join(", ", missing));
            }

            List<ExcelRow> rows = new ArrayList<>();
            for (int rowIndex = headerRow.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlank(row, formatter, evaluator)) {
                    continue;
                }
                if (rows.size() >= MAX_DATA_ROWS) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Excel imports are limited to 5,000 data rows.");
                }
                Map<String, String> values = new HashMap<>();
                headerByColumn.forEach((column, header) -> {
                    String value = formatter.formatCellValue(row.getCell(column), evaluator).trim();
                    values.put(header, value);
                });
                rows.add(new ExcelRow(rowIndex + 1, Map.copyOf(values)));
            }
            if (rows.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "The Excel file contains no data rows.");
            }
            return List.copyOf(rows);
        } catch (ApiException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read this Excel file. Check the file format and try again.");
        }
    }

    private boolean isBlank(Row row, DataFormatter formatter, org.apache.poi.ss.usermodel.FormulaEvaluator evaluator) {
        for (int column = row.getFirstCellNum(); column >= 0 && column < row.getLastCellNum(); column++) {
            if (!formatter.formatCellValue(row.getCell(column), evaluator).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String normalizeHeader(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return switch (normalized) {
            case "usn", "uucms", "registrationnumber", "regno", "registerno" -> "registernumber";
            case "student" -> "studentname";
            case "batch", "coursebatch", "coursebatchlabel" -> "batchlabel";
            case "qp", "questionpapercode" -> "qpcode";
            case "yearmonth", "exammonth", "monthyear" -> "examyearmonth";
            case "examinationdate", "dateofexam" -> "examdate";
            case "center", "centercode", "centre", "centrecode" -> "centrecode";
            default -> normalized;
        };
    }

    public record ExcelRow(int rowNumber, Map<String, String> values) {
        public String get(String key) {
            return values.getOrDefault(key, "");
        }
    }
}
