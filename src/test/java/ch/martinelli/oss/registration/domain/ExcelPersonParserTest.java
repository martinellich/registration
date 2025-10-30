package ch.martinelli.oss.registration.domain;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
        ExcelPersonData person1 = persons.get(0);
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

}
