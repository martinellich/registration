package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.registration.TestcontainersConfiguration;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static ch.martinelli.oss.registration.db.tables.EventRegistration.EVENT_REGISTRATION;
import static ch.martinelli.oss.registration.db.tables.Person.PERSON;
import static ch.martinelli.oss.registration.db.tables.RegistrationEmailPerson.REGISTRATION_EMAIL_PERSON;
import static ch.martinelli.oss.registration.db.tables.RegistrationPerson.REGISTRATION_PERSON;
import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class PersonChangeDetectorTest {

    @Autowired
    private DSLContext dslContext;

    @Autowired
    private PersonChangeDetector detector;

    @BeforeEach
    void setUp() {
        // Clean up test data to ensure empty database for these tests
        // Delete in correct order to respect foreign key constraints
        // @Transactional will roll back these changes after the test
        dslContext.deleteFrom(EVENT_REGISTRATION).execute();
        dslContext.deleteFrom(REGISTRATION_EMAIL_PERSON).execute();
        dslContext.deleteFrom(REGISTRATION_PERSON).execute();
        dslContext.deleteFrom(PERSON).execute();
    }

    @Test
    void shouldDetectNewPerson() {
        // Given - empty database
        var newPerson = new ExcelPersonData(123, "John", "Doe", "john@example.com");

        // When
        var changes = detector.detectChanges(List.of(newPerson));

        // Then
        assertThat(changes).hasSize(1);
        var change = changes.getFirst();
        assertThat(change.getType()).isEqualTo(PersonChange.ChangeType.NEW);
        assertThat(change.getExistingRecord()).isNull();
        assertThat(change.getNewData()).isEqualTo(newPerson);
        assertThat(change.getChangedFields()).containsKeys("firstName", "lastName", "email", "memberId");
        assertThat(change.isAccepted()).isTrue(); // Default to accepted
    }

    @Test
    void shouldDetectUpdateByMemberId() {
        // Given
        dslContext.insertInto(PERSON)
            .set(PERSON.MEMBER_ID, 123)
            .set(PERSON.FIRST_NAME, "John")
            .set(PERSON.LAST_NAME, "Doe")
            .set(PERSON.EMAIL, "old@example.com")
            .set(PERSON.ACTIVE, true)
            .execute();

        var updatedData = new ExcelPersonData(123, "John", "Doe", "new@example.com");

        // When
        var changes = detector.detectChanges(List.of(updatedData));

        // Then
        assertThat(changes).hasSize(1);
        var change = changes.getFirst();
        assertThat(change.getType()).isEqualTo(PersonChange.ChangeType.UPDATE);
        assertThat(change.getExistingRecord().getMemberId()).isEqualTo(123);
        assertThat(change.getNewData()).isEqualTo(updatedData);
        assertThat(change.getChangedFields()).containsOnlyKeys("email");
        assertThat(change.getChangedFields().get("email").oldValue()).isEqualTo("old@example.com");
        assertThat(change.getChangedFields().get("email").newValue()).isEqualTo("new@example.com");
    }

    @Test
    void shouldDetectUpdateByFirstNameAndLastName() {
        // Given - person without member ID
        dslContext.insertInto(PERSON)
            .set(PERSON.FIRST_NAME, "John")
            .set(PERSON.LAST_NAME, "Doe")
            .set(PERSON.EMAIL, "old@example.com")
            .set(PERSON.ACTIVE, true)
            .execute();

        var updatedData = new ExcelPersonData(null, "John", "Doe", "new@example.com");

        // When
        var changes = detector.detectChanges(List.of(updatedData));

        // Then
        assertThat(changes).hasSize(1);
        var change = changes.getFirst();
        assertThat(change.getType()).isEqualTo(PersonChange.ChangeType.UPDATE);
        assertThat(change.getExistingRecord().getFirstName()).isEqualTo("John");
        assertThat(change.getExistingRecord().getLastName()).isEqualTo("Doe");
    }

    @Test
    void shouldNotReturnNoChanges() {
        // Given - identical data
        dslContext.insertInto(PERSON)
            .set(PERSON.MEMBER_ID, 123)
            .set(PERSON.FIRST_NAME, "John")
            .set(PERSON.LAST_NAME, "Doe")
            .set(PERSON.EMAIL, "john@example.com")
            .set(PERSON.ACTIVE, true)
            .execute();

        var sameData = new ExcelPersonData(123, "John", "Doe", "john@example.com");

        // When
        var changes = detector.detectChanges(List.of(sameData));

        // Then - NO_CHANGE should be filtered out
        assertThat(changes).isEmpty();
    }

    @Test
    void shouldDetectMultipleFieldChanges() {
        // Given - person matched by member ID with multiple field changes
        dslContext.insertInto(PERSON)
            .set(PERSON.MEMBER_ID, 123)
            .set(PERSON.FIRST_NAME, "John")
            .set(PERSON.LAST_NAME, "Doe")
            .set(PERSON.EMAIL, "old@example.com")
            .set(PERSON.ACTIVE, true)
            .execute();

        // Same member ID but different other fields
        var updatedData = new ExcelPersonData(123, "Jane", "Smith", "new@example.com");

        // When
        var changes = detector.detectChanges(List.of(updatedData));

        // Then
        assertThat(changes).hasSize(1);
        var change = changes.getFirst();
        assertThat(change.getType()).isEqualTo(PersonChange.ChangeType.UPDATE);
        assertThat(change.getChangedFields()).containsKeys("firstName", "lastName", "email");
    }

    @Test
    void shouldPreferMemberIdMatchOverNameMatch() {
        // Given - two persons, one with matching member ID, one with matching name
        dslContext.insertInto(PERSON)
            .set(PERSON.MEMBER_ID, 123)
            .set(PERSON.FIRST_NAME, "John")
            .set(PERSON.LAST_NAME, "Doe")
            .set(PERSON.EMAIL, "john@example.com")
            .set(PERSON.ACTIVE, true)
            .execute();

        dslContext.insertInto(PERSON)
            .set(PERSON.MEMBER_ID, 456)
            .set(PERSON.FIRST_NAME, "Jane")
            .set(PERSON.LAST_NAME, "Smith")
            .set(PERSON.EMAIL, "jane@example.com")
            .set(PERSON.ACTIVE, true)
            .execute();

        // Excel data matches person1 by member ID but has different name
        var excelData = new ExcelPersonData(123, "Jane", "Smith", "updated@example.com");

        // When
        var changes = detector.detectChanges(List.of(excelData));

        // Then - should match person1 by member ID (not person2 by name) and deactivate
        // person2
        assertThat(changes).hasSize(2);
        assertThat(changes).extracting(PersonChange::getType)
            .containsExactlyInAnyOrder(PersonChange.ChangeType.UPDATE, PersonChange.ChangeType.DEACTIVATE);

        var updateChange = changes.stream()
            .filter(c -> c.getType() == PersonChange.ChangeType.UPDATE)
            .findFirst()
            .orElseThrow();
        assertThat(updateChange.getExistingRecord().getMemberId()).isEqualTo(123);

        var deactivateChange = changes.stream()
            .filter(c -> c.getType() == PersonChange.ChangeType.DEACTIVATE)
            .findFirst()
            .orElseThrow();
        assertThat(deactivateChange.getExistingRecord().getMemberId()).isEqualTo(456);
    }

    @Test
    void shouldDetectActivePersonsNotInExcelForDeactivation() {
        // Given - active person in database but not in Excel
        dslContext.insertInto(PERSON)
            .set(PERSON.MEMBER_ID, 123)
            .set(PERSON.FIRST_NAME, "John")
            .set(PERSON.LAST_NAME, "Doe")
            .set(PERSON.EMAIL, "john@example.com")
            .set(PERSON.ACTIVE, true)
            .execute();

        dslContext.insertInto(PERSON)
            .set(PERSON.MEMBER_ID, 456)
            .set(PERSON.FIRST_NAME, "Jane")
            .set(PERSON.LAST_NAME, "Smith")
            .set(PERSON.EMAIL, "jane@example.com")
            .set(PERSON.ACTIVE, false)
            .execute();

        // When - Excel is empty (neither person is in the Excel)
        var changes = detector.detectChanges(List.of());

        // Then - only active person should be marked for deactivation
        assertThat(changes).hasSize(1);
        var change = changes.getFirst();
        assertThat(change.getType()).isEqualTo(PersonChange.ChangeType.DEACTIVATE);
        assertThat(change.getExistingRecord().getMemberId()).isEqualTo(123);
        assertThat(change.getNewData()).isNull();
        assertThat(change.getChangedFields()).containsOnlyKeys("active");
        assertThat(change.getChangedFields().get("active").oldValue()).isEqualTo("true");
        assertThat(change.getChangedFields().get("active").newValue()).isEqualTo("false");
    }

    @Test
    void shouldCombineNewUpdateAndDeactivateChanges() {
        // Given - three persons in database
        dslContext.insertInto(PERSON)
            .set(PERSON.MEMBER_ID, 123)
            .set(PERSON.FIRST_NAME, "John")
            .set(PERSON.LAST_NAME, "Doe")
            .set(PERSON.EMAIL, "old@example.com")
            .set(PERSON.ACTIVE, true)
            .execute();

        dslContext.insertInto(PERSON)
            .set(PERSON.MEMBER_ID, 456)
            .set(PERSON.FIRST_NAME, "Jane")
            .set(PERSON.LAST_NAME, "Smith")
            .set(PERSON.EMAIL, "jane@example.com")
            .set(PERSON.ACTIVE, true)
            .execute();

        dslContext.insertInto(PERSON)
            .set(PERSON.MEMBER_ID, 789)
            .set(PERSON.FIRST_NAME, "Bob")
            .set(PERSON.LAST_NAME, "Wilson")
            .set(PERSON.EMAIL, "bob@example.com")
            .set(PERSON.ACTIVE, false)
            .execute();

        // When - Excel contains one update and one new person
        var updateData = new ExcelPersonData(123, "John", "Doe", "new@example.com");
        var newPersonData = new ExcelPersonData(999, "Alice", "Johnson", "alice@example.com");
        var changes = detector.detectChanges(List.of(updateData, newPersonData));

        // Then - should have 1 UPDATE, 1 NEW, 1 DEACTIVATE
        assertThat(changes).hasSize(3);
        assertThat(changes).extracting(PersonChange::getType)
            .containsExactlyInAnyOrder(PersonChange.ChangeType.UPDATE, PersonChange.ChangeType.NEW,
                    PersonChange.ChangeType.DEACTIVATE);

        // Verify UPDATE
        var updateChange = changes.stream()
            .filter(c -> c.getType() == PersonChange.ChangeType.UPDATE)
            .findFirst()
            .orElseThrow();
        assertThat(updateChange.getExistingRecord().getMemberId()).isEqualTo(123);

        // Verify NEW
        var newChange = changes.stream()
            .filter(c -> c.getType() == PersonChange.ChangeType.NEW)
            .findFirst()
            .orElseThrow();
        assertThat(newChange.getNewData()).isEqualTo(newPersonData);

        // Verify DEACTIVATE (only active unmatched person)
        var deactivateChange = changes.stream()
            .filter(c -> c.getType() == PersonChange.ChangeType.DEACTIVATE)
            .findFirst()
            .orElseThrow();
        assertThat(deactivateChange.getExistingRecord().getMemberId()).isEqualTo(456);
    }

}
