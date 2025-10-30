package ch.martinelli.oss.registration.domain;

/**
 * DTO representing a person parsed from Excel file. Contains the raw data extracted from
 * Excel columns.
 */
public record ExcelPersonData(Integer memberId, String firstName, String lastName, String email) {
}
