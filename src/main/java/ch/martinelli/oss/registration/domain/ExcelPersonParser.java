package ch.martinelli.oss.registration.domain;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to parse person data from Excel files. Expected Excel format: - First row is
 * header - Columns: MITGLIEDERNR, VORNAME, NACHNAME, STRASSE, PLZ, ORT, EMAIL, EMAIL
 * ALTERNATIV, KATEGORIE, ZUSATZ, RIEGEN
 */
@Service
public class ExcelPersonParser {

    private static final int COL_MEMBER_ID = 0;

    private static final int COL_FIRST_NAME = 1;

    private static final int COL_LAST_NAME = 2;

    private static final int COL_EMAIL = 6;

    private static final int COL_EMAIL_ALT = 7;

    /**
     * Parse Excel file and extract person data.
     * @param inputStream Excel file input stream
     * @return List of parsed person data
     * @throws IOException if file cannot be read or is invalid
     */
    public List<ExcelPersonData> parseExcelFile(InputStream inputStream) throws IOException {
        List<ExcelPersonData> persons = new ArrayList<>();

        try (var workbook = new XSSFWorkbook(inputStream)) {
            var sheet = workbook.getSheetAt(0);

            // Skip header row (row 0)
            for (var i = 1; i <= sheet.getLastRowNum(); i++) {
                var row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                var personData = parseRow(row);
                if (personData != null) {
                    persons.add(personData);
                }
            }
        }

        return persons;
    }

    private ExcelPersonData parseRow(Row row) {
        // Extract member ID (numeric in Excel, convert to Integer)
        Integer memberId = null;
        var memberIdCell = row.getCell(COL_MEMBER_ID);
        if (memberIdCell != null && memberIdCell.getCellType() == CellType.NUMERIC) {
            memberId = (int) memberIdCell.getNumericCellValue();
        }

        // Extract first name
        var firstName = getCellValueAsString(row.getCell(COL_FIRST_NAME));

        // Extract last name
        var lastName = getCellValueAsString(row.getCell(COL_LAST_NAME));

        // Extract email (prefer EMAIL, fall back to EMAIL ALTERNATIV)
        var email = getCellValueAsString(row.getCell(COL_EMAIL));
        if (email == null || email.isBlank()) {
            email = getCellValueAsString(row.getCell(COL_EMAIL_ALT));
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
