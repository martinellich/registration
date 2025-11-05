package ch.martinelli.oss.registration.domain;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to parse person data from Excel files. Expected Excel format: - First row
 * is header - Columns: MITGLIEDERNR, VORNAME, NACHNAME, STRASSE, PLZ, ORT, EMAIL, EMAIL
 * ALTERNATIV, KATEGORIE, ZUSATZ, RIEGEN
 * <p>
 * The parser reads the header row to determine column positions dynamically, allowing
 * columns to be in any order.
 */
public class ExcelPersonParser {

    // Expected header names (case-insensitive)
    private static final String HEADER_MEMBER_ID = "MITGLIEDERNR";

    private static final String HEADER_FIRST_NAME = "VORNAME";

    private static final String HEADER_LAST_NAME = "NACHNAME";

    private static final String HEADER_EMAIL = "EMAIL";

    private static final String HEADER_EMAIL_ALT = "EMAIL ALTERNATIV";

    /**
     * Parse Excel file and extract person data.
     * @param inputStream Excel file input stream
     * @return List of parsed person data
     * @throws IOException if file cannot be read or is invalid
     * @throws IllegalArgumentException if required headers are missing
     */
    public List<ExcelPersonData> parseExcelFile(InputStream inputStream) throws IOException {
        List<ExcelPersonData> persons = new ArrayList<>();

        try (var workbook = new XSSFWorkbook(inputStream)) {
            var sheet = workbook.getSheetAt(0);

            // Parse header row to determine column positions
            var headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return persons; // Empty sheet
            }

            var columnMap = parseHeaderRow(headerRow);

            // Validate required columns exist
            if (!columnMap.containsKey(HEADER_FIRST_NAME) || !columnMap.containsKey(HEADER_LAST_NAME)) {
                throw new IllegalArgumentException(
                        "Required headers missing. Expected: " + HEADER_FIRST_NAME + ", " + HEADER_LAST_NAME);
            }

            // Parse data rows (starting from row 1)
            for (var i = 1; i <= sheet.getLastRowNum(); i++) {
                var row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                var personData = parseRow(row, columnMap);
                if (personData != null) {
                    persons.add(personData);
                }
            }
        }

        return persons;
    }

    /**
     * Parse Excel file and extract person data from byte array.
     * @param data Excel file as byte array
     * @return List of parsed person data
     * @throws IOException if file cannot be read or is invalid
     * @throws IllegalArgumentException if required headers are missing
     */
    public List<ExcelPersonData> parseExcelFile(byte[] data) throws IOException {
        return parseExcelFile(new ByteArrayInputStream(data));
    }

    /**
     * Parse header row and create a map of column names to indices.
     * @param headerRow The header row from the Excel sheet
     * @return Map of normalized column names to column indices
     */
    private Map<String, Integer> parseHeaderRow(Row headerRow) {
        Map<String, Integer> columnMap = new HashMap<>();

        for (var i = 0; i < headerRow.getLastCellNum(); i++) {
            var cell = headerRow.getCell(i);
            if (cell != null) {
                var headerName = getCellValueAsString(cell);
                if (headerName != null && !headerName.isBlank()) {
                    // Normalize: uppercase and trim
                    var normalizedName = headerName.toUpperCase().trim();
                    columnMap.put(normalizedName, i);
                }
            }
        }

        return columnMap;
    }

    private ExcelPersonData parseRow(Row row, Map<String, Integer> columnMap) {
        // Extract member ID (numeric in Excel, convert to Integer)
        Integer memberId = null;
        var memberIdIndex = columnMap.get(HEADER_MEMBER_ID);
        if (memberIdIndex != null) {
            var memberIdCell = row.getCell(memberIdIndex);
            if (memberIdCell != null && memberIdCell.getCellType() == CellType.NUMERIC) {
                memberId = (int) memberIdCell.getNumericCellValue();
            }
        }

        // Extract first name
        var firstNameIndex = columnMap.get(HEADER_FIRST_NAME);
        var firstName = firstNameIndex != null ? getCellValueAsString(row.getCell(firstNameIndex)) : null;

        // Extract last name
        var lastNameIndex = columnMap.get(HEADER_LAST_NAME);
        var lastName = lastNameIndex != null ? getCellValueAsString(row.getCell(lastNameIndex)) : null;

        // Extract email (prefer EMAIL, fall back to EMAIL ALTERNATIV)
        var emailIndex = columnMap.get(HEADER_EMAIL);
        var email = emailIndex != null ? getCellValueAsString(row.getCell(emailIndex)) : null;
        if (email == null || email.isBlank()) {
            var emailAltIndex = columnMap.get(HEADER_EMAIL_ALT);
            email = emailAltIndex != null ? getCellValueAsString(row.getCell(emailAltIndex)) : null;
        }

        // Skip rows without required fields
        if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
            return null;
        }

        return new ExcelPersonData(memberId, firstName, lastName, email);
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getRichStringCellValue().getString().trim();
            default -> null;
        };
    }

    private boolean isRowEmpty(Row row) {
        for (var i = 0; i < row.getLastCellNum(); i++) {
            var cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

}
