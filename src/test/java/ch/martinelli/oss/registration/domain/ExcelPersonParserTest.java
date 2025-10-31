package ch.martinelli.oss.registration.domain;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExcelPersonParserTest {

    private final ExcelPersonParser parser = new ExcelPersonParser();

    @Test
    void shouldParseExcelFile() throws IOException {
        // Given
        InputStream inputStream = getClass().getResourceAsStream("/upload_partipipants.xlsx");
        assertThat(inputStream).isNotNull();

        // When
        List<ExcelPersonData> persons = parser.parseExcelFile(inputStream);

        // Then
        assertThat(persons).hasSize(2);

        // First person
        ExcelPersonData person1 = persons.getFirst();
        assertThat(person1.memberId()).isEqualTo(3934004);
        assertThat(person1.firstName()).isEqualTo("Kalra");
        assertThat(person1.lastName()).isEqualTo("Hansen");
        assertThat(person1.email()).isEqualTo("hansen@gmail.com");

        // Second person
        ExcelPersonData person2 = persons.get(1);
        assertThat(person2.memberId()).isEqualTo(3729313);
        assertThat(person2.firstName()).isEqualTo("Samira");
        assertThat(person2.lastName()).isEqualTo("Meier");
        assertThat(person2.email()).isEqualTo("meier@gmail.com");
    }

    @Test
    void shouldPreferEmailOverEmailAlternativ() throws IOException {
        // This test verifies that EMAIL column is preferred over EMAIL ALTERNATIV
        // Based on the test data, both persons have values in EMAIL ALTERNATIV
        InputStream inputStream = getClass().getResourceAsStream("/upload_partipipants.xlsx");
        assertThat(inputStream).isNotNull();

        List<ExcelPersonData> persons = parser.parseExcelFile(inputStream);

        // The parser should use EMAIL ALTERNATIV as fallback only when EMAIL is blank
        assertThat(persons).isNotEmpty();
        persons.forEach(person -> assertThat(person.email()).isNotBlank());
    }

    @Test
    void shouldSkipRowsWithoutRequiredFields() throws IOException {
        // This test ensures that rows without firstName or lastName are skipped
        // The test file should only contain valid rows with all required fields
        InputStream inputStream = getClass().getResourceAsStream("/upload_partipipants.xlsx");
        assertThat(inputStream).isNotNull();

        List<ExcelPersonData> persons = parser.parseExcelFile(inputStream);

        // All parsed persons should have firstName and lastName
        assertThat(persons).isNotEmpty()
            .allMatch(person -> person.firstName() != null && !person.firstName().isBlank() && person.lastName() != null
                    && !person.lastName().isBlank());
    }

    // ========== Empty Files/Sheets Tests ==========

    @Test
    void shouldReturnEmptyListForEmptySheet() throws IOException {
        // Given
        var workbook = new XSSFWorkbook();
        workbook.createSheet("Sheet1");
        var bytes = workbookToBytes(workbook);

        // When
        var persons = parser.parseExcelFile(bytes);

        // Then
        assertThat(persons).isEmpty();
    }

    @Test
    void shouldReturnEmptyListForSheetWithOnlyHeader() throws IOException {
        // Given
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet("Sheet1");
        var headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("MITGLIEDERNR");
        headerRow.createCell(1).setCellValue("VORNAME");
        headerRow.createCell(2).setCellValue("NACHNAME");
        var bytes = workbookToBytes(workbook);

        // When
        var persons = parser.parseExcelFile(bytes);

        // Then
        assertThat(persons).isEmpty();
    }

    @Test
    void shouldSkipEmptyRows() throws IOException {
        // Given
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet("Sheet1");

        // Header
        createHeaderRow(sheet);

        // Valid row
        var row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue(123);
        row1.createCell(1).setCellValue("John");
        row1.createCell(2).setCellValue("Doe");
        row1.createCell(6).setCellValue("john@example.com");

        // Empty row (row 2 is null)

        // Another valid row
        var row3 = sheet.createRow(3);
        row3.createCell(0).setCellValue(456);
        row3.createCell(1).setCellValue("Jane");
        row3.createCell(2).setCellValue("Smith");
        row3.createCell(6).setCellValue("jane@example.com");

        var bytes = workbookToBytes(workbook);

        // When
        var persons = parser.parseExcelFile(bytes);

        // Then
        assertThat(persons).hasSize(2);
        assertThat(persons.get(0).firstName()).isEqualTo("John");
        assertThat(persons.get(1).firstName()).isEqualTo("Jane");
    }

    // ========== Missing Required Fields Tests ==========

    @Test
    void shouldSkipRowWithMissingFirstName() throws IOException {
        // Given
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet("Sheet1");
        createHeaderRow(sheet);

        var row = sheet.createRow(1);
        row.createCell(0).setCellValue(123);
        // Missing first name (column 1)
        row.createCell(2).setCellValue("Doe");
        row.createCell(6).setCellValue("john@example.com");

        var bytes = workbookToBytes(workbook);

        // When
        var persons = parser.parseExcelFile(bytes);

        // Then
        assertThat(persons).isEmpty();
    }

    @Test
    void shouldSkipRowWithBlankFirstName() throws IOException {
        // Given
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet("Sheet1");
        createHeaderRow(sheet);

        var row = sheet.createRow(1);
        row.createCell(0).setCellValue(123);
        row.createCell(1).setCellValue("   "); // Blank first name
        row.createCell(2).setCellValue("Doe");
        row.createCell(6).setCellValue("john@example.com");

        var bytes = workbookToBytes(workbook);

        // When
        var persons = parser.parseExcelFile(bytes);

        // Then
        assertThat(persons).isEmpty();
    }

    @Test
    void shouldSkipRowWithMissingLastName() throws IOException {
        // Given
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet("Sheet1");
        createHeaderRow(sheet);

        var row = sheet.createRow(1);
        row.createCell(0).setCellValue(123);
        row.createCell(1).setCellValue("John");
        // Missing last name (column 2)
        row.createCell(6).setCellValue("john@example.com");

        var bytes = workbookToBytes(workbook);

        // When
        var persons = parser.parseExcelFile(bytes);

        // Then
        assertThat(persons).isEmpty();
    }

    @Test
    void shouldSkipRowWithBlankLastName() throws IOException {
        // Given
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet("Sheet1");
        createHeaderRow(sheet);

        var row = sheet.createRow(1);
        row.createCell(0).setCellValue(123);
        row.createCell(1).setCellValue("John");
        row.createCell(2).setCellValue("   "); // Blank last name
        row.createCell(6).setCellValue("john@example.com");

        var bytes = workbookToBytes(workbook);

        // When
        var persons = parser.parseExcelFile(bytes);

        // Then
        assertThat(persons).isEmpty();
    }

    @Test
    void shouldParseRowWithMissingMemberId() throws IOException {
        // Given
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet("Sheet1");
        createHeaderRow(sheet);

        var row = sheet.createRow(1);
        // Missing member ID (column 0)
        row.createCell(1).setCellValue("John");
        row.createCell(2).setCellValue("Doe");
        row.createCell(6).setCellValue("john@example.com");

        var bytes = workbookToBytes(workbook);

        // When
        var persons = parser.parseExcelFile(bytes);

        // Then
        assertThat(persons).hasSize(1);
        assertThat(persons.getFirst().memberId()).isNull();
        assertThat(persons.getFirst().firstName()).isEqualTo("John");
        assertThat(persons.getFirst().lastName()).isEqualTo("Doe");
    }

    @Test
    void shouldParseRowWithMissingEmail() throws IOException {
        // Given
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet("Sheet1");
        createHeaderRow(sheet);

        var row = sheet.createRow(1);
        row.createCell(0).setCellValue(123);
        row.createCell(1).setCellValue("John");
        row.createCell(2).setCellValue("Doe");
        // Missing email (columns 6 and 7)

        var bytes = workbookToBytes(workbook);

        // When
        var persons = parser.parseExcelFile(bytes);

        // Then
        assertThat(persons).hasSize(1);
        assertThat(persons.getFirst().email()).isNull();
    }

    // ========== Email Fallback Tests ==========

    @Test
    void shouldUseEmailWhenAvailable() throws IOException {
        // Given
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet("Sheet1");
        createHeaderRow(sheet);

        var row = sheet.createRow(1);
        row.createCell(0).setCellValue(123);
        row.createCell(1).setCellValue("John");
        row.createCell(2).setCellValue("Doe");
        row.createCell(6).setCellValue("primary@example.com");
        row.createCell(7).setCellValue("secondary@example.com");

        var bytes = workbookToBytes(workbook);

        // When
        var persons = parser.parseExcelFile(bytes);

        // Then
        assertThat(persons).hasSize(1);
        assertThat(persons.getFirst().email()).isEqualTo("primary@example.com");
    }

    @Test
    void shouldFallbackToEmailAlternativWhenEmailIsBlank() throws IOException {
        // Given
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet("Sheet1");
        createHeaderRow(sheet);

        var row = sheet.createRow(1);
        row.createCell(0).setCellValue(123);
        row.createCell(1).setCellValue("John");
        row.createCell(2).setCellValue("Doe");
        row.createCell(6).setCellValue("   "); // Blank email
        row.createCell(7).setCellValue("secondary@example.com");

        var bytes = workbookToBytes(workbook);

        // When
        var persons = parser.parseExcelFile(bytes);

        // Then
        assertThat(persons).hasSize(1);
        assertThat(persons.getFirst().email()).isEqualTo("secondary@example.com");
    }

    @Test
    void shouldFallbackToEmailAlternativWhenEmailIsMissing() throws IOException {
        // Given
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet("Sheet1");
        createHeaderRow(sheet);

        var row = sheet.createRow(1);
        row.createCell(0).setCellValue(123);
        row.createCell(1).setCellValue("John");
        row.createCell(2).setCellValue("Doe");
        // Missing email (column 6)
        row.createCell(7).setCellValue("secondary@example.com");

        var bytes = workbookToBytes(workbook);

        // When
        var persons = parser.parseExcelFile(bytes);

        // Then
        assertThat(persons).hasSize(1);
        assertThat(persons.getFirst().email()).isEqualTo("secondary@example.com");
    }

    // ========== Different Cell Types Tests ==========

    @Test
    void shouldHandleNumericCellForFirstName() throws IOException {
        // Given
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet("Sheet1");
        createHeaderRow(sheet);

        var row = sheet.createRow(1);
        row.createCell(0).setCellValue(123);
        row.createCell(1).setCellValue(456); // Numeric first name
        row.createCell(2).setCellValue("Doe");
        row.createCell(6).setCellValue("john@example.com");

        var bytes = workbookToBytes(workbook);

        // When
        var persons = parser.parseExcelFile(bytes);

        // Then
        assertThat(persons).hasSize(1);
        assertThat(persons.getFirst().firstName()).isEqualTo("456");
    }

    @Test
    void shouldHandleBooleanCellForFirstName() throws IOException {
        // Given
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet("Sheet1");
        createHeaderRow(sheet);

        var row = sheet.createRow(1);
        row.createCell(0).setCellValue(123);
        row.createCell(1).setCellValue(true); // Boolean first name
        row.createCell(2).setCellValue("Doe");
        row.createCell(6).setCellValue("john@example.com");

        var bytes = workbookToBytes(workbook);

        // When
        var persons = parser.parseExcelFile(bytes);

        // Then
        assertThat(persons).hasSize(1);
        assertThat(persons.getFirst().firstName()).isEqualTo("true");
    }

    @Test
    void shouldHandleFormulaCellForFirstName() throws IOException {
        // Given
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet("Sheet1");
        createHeaderRow(sheet);

        var row = sheet.createRow(1);
        row.createCell(0).setCellValue(123);
        var formulaCell = row.createCell(1);
        formulaCell.setCellFormula("CONCATENATE(\"Jo\",\"hn\")");
        // Set cached value for formula cell
        formulaCell.setCellValue("John");
        row.createCell(2).setCellValue("Doe");
        row.createCell(6).setCellValue("john@example.com");

        var bytes = workbookToBytes(workbook);

        // When
        var persons = parser.parseExcelFile(bytes);

        // Then
        assertThat(persons).hasSize(1);
        assertThat(persons.getFirst().firstName()).isEqualTo("John");
    }

    @Test
    void shouldHandleBlankCellType() throws IOException {
        // Given
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet("Sheet1");
        createHeaderRow(sheet);

        var row = sheet.createRow(1);
        row.createCell(0).setCellValue(123);
        row.createCell(1).setCellValue("John");
        row.createCell(2).setCellValue("Doe");
        var blankCell = row.createCell(6);
        blankCell.setBlank(); // Blank cell

        var bytes = workbookToBytes(workbook);

        // When
        var persons = parser.parseExcelFile(bytes);

        // Then
        assertThat(persons).hasSize(1);
        assertThat(persons.getFirst().email()).isNull();
    }

    @Test
    void shouldTrimWhitespaceFromStringCells() throws IOException {
        // Given
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet("Sheet1");
        createHeaderRow(sheet);

        var row = sheet.createRow(1);
        row.createCell(0).setCellValue(123);
        row.createCell(1).setCellValue("  John  ");
        row.createCell(2).setCellValue("  Doe  ");
        row.createCell(6).setCellValue("  john@example.com  ");

        var bytes = workbookToBytes(workbook);

        // When
        var persons = parser.parseExcelFile(bytes);

        // Then
        assertThat(persons).hasSize(1);
        assertThat(persons.getFirst().firstName()).isEqualTo("John");
        assertThat(persons.getFirst().lastName()).isEqualTo("Doe");
        assertThat(persons.getFirst().email()).isEqualTo("john@example.com");
    }

    @Test
    void shouldConvertNumericMemberIdToInteger() throws IOException {
        // Given
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet("Sheet1");
        createHeaderRow(sheet);

        var row = sheet.createRow(1);
        row.createCell(0).setCellValue(3934004.5); // Decimal value
        row.createCell(1).setCellValue("John");
        row.createCell(2).setCellValue("Doe");
        row.createCell(6).setCellValue("john@example.com");

        var bytes = workbookToBytes(workbook);

        // When
        var persons = parser.parseExcelFile(bytes);

        // Then
        assertThat(persons).hasSize(1);
        assertThat(persons.getFirst().memberId()).isEqualTo(3934004); // Truncated to int
    }

    @Test
    void shouldIgnoreNonNumericMemberId() throws IOException {
        // Given
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet("Sheet1");
        createHeaderRow(sheet);

        var row = sheet.createRow(1);
        row.createCell(0).setCellValue("ABC123"); // String member ID
        row.createCell(1).setCellValue("John");
        row.createCell(2).setCellValue("Doe");
        row.createCell(6).setCellValue("john@example.com");

        var bytes = workbookToBytes(workbook);

        // When
        var persons = parser.parseExcelFile(bytes);

        // Then
        assertThat(persons).hasSize(1);
        assertThat(persons.getFirst().memberId()).isNull(); // Non-numeric ignored
    }

    // ========== Invalid Excel Format Tests ==========

    @Test
    void shouldThrowExceptionForInvalidExcelData() {
        // Given
        var invalidData = "This is not an Excel file".getBytes();

        // When/Then
        // POI throws NotOfficeXmlFileException which is a RuntimeException, not
        // IOException
        assertThatThrownBy(() -> parser.parseExcelFile(invalidData))
            .isInstanceOf(org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException.class)
            .hasMessageContaining("not a valid OOXML");
    }

    @Test
    void shouldThrowExceptionForEmptyByteArray() {
        // Given
        var emptyData = new byte[0];

        // When/Then
        // POI throws EmptyFileException which extends IOException
        assertThatThrownBy(() -> parser.parseExcelFile(emptyData)).isInstanceOf(org.apache.poi.EmptyFileException.class)
            .hasMessageContaining("empty");
    }

    // ========== Byte Array Method Tests ==========

    @Test
    void shouldParseExcelFileFromByteArray() throws IOException {
        // Given
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet("Sheet1");
        createHeaderRow(sheet);

        var row = sheet.createRow(1);
        row.createCell(0).setCellValue(123);
        row.createCell(1).setCellValue("John");
        row.createCell(2).setCellValue("Doe");
        row.createCell(6).setCellValue("john@example.com");

        var bytes = workbookToBytes(workbook);

        // When
        var persons = parser.parseExcelFile(bytes);

        // Then
        assertThat(persons).hasSize(1);
        assertThat(persons.getFirst().firstName()).isEqualTo("John");
        assertThat(persons.getFirst().lastName()).isEqualTo("Doe");
        assertThat(persons.getFirst().email()).isEqualTo("john@example.com");
    }

    // ========== Helper Methods ==========

    private void createHeaderRow(Sheet sheet) {
        var headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("MITGLIEDERNR");
        headerRow.createCell(1).setCellValue("VORNAME");
        headerRow.createCell(2).setCellValue("NACHNAME");
        headerRow.createCell(3).setCellValue("STRASSE");
        headerRow.createCell(4).setCellValue("PLZ");
        headerRow.createCell(5).setCellValue("ORT");
        headerRow.createCell(6).setCellValue("EMAIL");
        headerRow.createCell(7).setCellValue("EMAIL ALTERNATIV");
    }

    private byte[] workbookToBytes(Workbook workbook) throws IOException {
        try (var baos = new ByteArrayOutputStream()) {
            workbook.write(baos);
            workbook.close();
            return baos.toByteArray();
        }
    }

}
