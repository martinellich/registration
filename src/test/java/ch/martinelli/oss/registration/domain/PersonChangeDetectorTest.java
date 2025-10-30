package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.registration.db.tables.records.PersonRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static ch.martinelli.oss.registration.db.tables.Person.PERSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonChangeDetectorTest {

    @Mock
    private PersonRepository personRepository;

    private PersonChangeDetector detector;

    @BeforeEach
    void setUp() {
        detector = new PersonChangeDetector(personRepository);
    }

    @Test
    void shouldDetectNewPerson() {
        // Given
        ExcelPersonData newPerson = new ExcelPersonData(123, "John", "Doe", "john@example.com");
        when(personRepository.findAll(any())).thenReturn(List.of());

        // When
        List<PersonChange> changes = detector.detectChanges(List.of(newPerson));

        // Then
        assertThat(changes).hasSize(1);
        PersonChange change = changes.get(0);
        assertThat(change.getType()).isEqualTo(PersonChange.ChangeType.NEW);
        assertThat(change.getExistingRecord()).isNull();
        assertThat(change.getNewData()).isEqualTo(newPerson);
        assertThat(change.getChangedFields()).containsKeys("firstName", "lastName", "email", "memberId");
        assertThat(change.isAccepted()).isTrue(); // Default to accepted
    }

    @Test
    void shouldDetectUpdateByMemberId() {
        // Given
        PersonRecord existingPerson = PERSON.newRecord();
        existingPerson.setId(1L);
        existingPerson.setMemberId(123);
        existingPerson.setFirstName("John");
        existingPerson.setLastName("Doe");
        existingPerson.setEmail("old@example.com");
        existingPerson.setActive(true);

        ExcelPersonData updatedData = new ExcelPersonData(123, "John", "Doe", "new@example.com");
        when(personRepository.findAll(any())).thenReturn(List.of(existingPerson));

        // When
        List<PersonChange> changes = detector.detectChanges(List.of(updatedData));

        // Then
        assertThat(changes).hasSize(1);
        PersonChange change = changes.get(0);
        assertThat(change.getType()).isEqualTo(PersonChange.ChangeType.UPDATE);
        assertThat(change.getExistingRecord()).isEqualTo(existingPerson);
        assertThat(change.getNewData()).isEqualTo(updatedData);
        assertThat(change.getChangedFields()).containsOnlyKeys("email");
        assertThat(change.getChangedFields().get("email").oldValue()).isEqualTo("old@example.com");
        assertThat(change.getChangedFields().get("email").newValue()).isEqualTo("new@example.com");
    }

    @Test
    void shouldDetectUpdateByFirstNameAndLastName() {
        // Given - person without member ID
        PersonRecord existingPerson = PERSON.newRecord();
        existingPerson.setId(1L);
        existingPerson.setFirstName("John");
        existingPerson.setLastName("Doe");
        existingPerson.setEmail("old@example.com");
        existingPerson.setActive(true);

        ExcelPersonData updatedData = new ExcelPersonData(null, "John", "Doe", "new@example.com");
        when(personRepository.findAll(any())).thenReturn(List.of(existingPerson));

        // When
        List<PersonChange> changes = detector.detectChanges(List.of(updatedData));

        // Then
        assertThat(changes).hasSize(1);
        PersonChange change = changes.get(0);
        assertThat(change.getType()).isEqualTo(PersonChange.ChangeType.UPDATE);
        assertThat(change.getExistingRecord()).isEqualTo(existingPerson);
    }

    @Test
    void shouldNotReturnNoChanges() {
        // Given - identical data
        PersonRecord existingPerson = PERSON.newRecord();
        existingPerson.setId(1L);
        existingPerson.setMemberId(123);
        existingPerson.setFirstName("John");
        existingPerson.setLastName("Doe");
        existingPerson.setEmail("john@example.com");
        existingPerson.setActive(true);

        ExcelPersonData sameData = new ExcelPersonData(123, "John", "Doe", "john@example.com");
        when(personRepository.findAll(any())).thenReturn(List.of(existingPerson));

        // When
        List<PersonChange> changes = detector.detectChanges(List.of(sameData));

        // Then - NO_CHANGE should be filtered out
        assertThat(changes).isEmpty();
    }

    @Test
    void shouldDetectMultipleFieldChanges() {
        // Given - person matched by member ID with multiple field changes
        PersonRecord existingPerson = PERSON.newRecord();
        existingPerson.setId(1L);
        existingPerson.setMemberId(123);
        existingPerson.setFirstName("John");
        existingPerson.setLastName("Doe");
        existingPerson.setEmail("old@example.com");
        existingPerson.setActive(true);

        // Same member ID but different other fields
        ExcelPersonData updatedData = new ExcelPersonData(123, "Jane", "Smith", "new@example.com");
        when(personRepository.findAll(any())).thenReturn(List.of(existingPerson));

        // When
        List<PersonChange> changes = detector.detectChanges(List.of(updatedData));

        // Then
        assertThat(changes).hasSize(1);
        PersonChange change = changes.get(0);
        assertThat(change.getType()).isEqualTo(PersonChange.ChangeType.UPDATE);
        assertThat(change.getChangedFields()).containsKeys("firstName", "lastName", "email");
    }

    @Test
    void shouldPreferMemberIdMatchOverNameMatch() {
        // Given - two persons, one with matching member ID, one with matching name
        PersonRecord person1 = PERSON.newRecord();
        person1.setId(1L);
        person1.setMemberId(123);
        person1.setFirstName("John");
        person1.setLastName("Doe");
        person1.setEmail("john@example.com");
        person1.setActive(true);

        PersonRecord person2 = PERSON.newRecord();
        person2.setId(2L);
        person2.setMemberId(456);
        person2.setFirstName("Jane");
        person2.setLastName("Smith");
        person2.setEmail("jane@example.com");
        person2.setActive(true);

        // Excel data matches person1 by member ID but has different name
        ExcelPersonData excelData = new ExcelPersonData(123, "Jane", "Smith", "updated@example.com");
        when(personRepository.findAll(any())).thenReturn(List.of(person1, person2));

        // When
        List<PersonChange> changes = detector.detectChanges(List.of(excelData));

        // Then - should match person1 by member ID (not person2 by name) and deactivate
        // person2
        assertThat(changes).hasSize(2);
        assertThat(changes).extracting(PersonChange::getType)
            .containsExactlyInAnyOrder(PersonChange.ChangeType.UPDATE, PersonChange.ChangeType.DEACTIVATE);

        PersonChange updateChange = changes.stream()
            .filter(c -> c.getType() == PersonChange.ChangeType.UPDATE)
            .findFirst()
            .orElseThrow();
        assertThat(updateChange.getExistingRecord()).isEqualTo(person1);

        PersonChange deactivateChange = changes.stream()
            .filter(c -> c.getType() == PersonChange.ChangeType.DEACTIVATE)
            .findFirst()
            .orElseThrow();
        assertThat(deactivateChange.getExistingRecord()).isEqualTo(person2);
    }

    @Test
    void shouldDetectActivePersonsNotInExcelForDeactivation() {
        // Given - active person in database but not in Excel
        PersonRecord activePerson = PERSON.newRecord();
        activePerson.setId(1L);
        activePerson.setMemberId(123);
        activePerson.setFirstName("John");
        activePerson.setLastName("Doe");
        activePerson.setEmail("john@example.com");
        activePerson.setActive(true);

        PersonRecord inactivePerson = PERSON.newRecord();
        inactivePerson.setId(2L);
        inactivePerson.setMemberId(456);
        inactivePerson.setFirstName("Jane");
        inactivePerson.setLastName("Smith");
        inactivePerson.setEmail("jane@example.com");
        inactivePerson.setActive(false);

        when(personRepository.findAll(any())).thenReturn(List.of(activePerson, inactivePerson));

        // When - Excel is empty (neither person is in the Excel)
        List<PersonChange> changes = detector.detectChanges(List.of());

        // Then - only active person should be marked for deactivation
        assertThat(changes).hasSize(1);
        PersonChange change = changes.get(0);
        assertThat(change.getType()).isEqualTo(PersonChange.ChangeType.DEACTIVATE);
        assertThat(change.getExistingRecord()).isEqualTo(activePerson);
        assertThat(change.getNewData()).isNull();
        assertThat(change.getChangedFields()).containsOnlyKeys("active");
        assertThat(change.getChangedFields().get("active").oldValue()).isEqualTo("true");
        assertThat(change.getChangedFields().get("active").newValue()).isEqualTo("false");
    }

    @Test
    void shouldCombineNewUpdateAndDeactivateChanges() {
        // Given - three persons in database
        PersonRecord matchedPerson = PERSON.newRecord();
        matchedPerson.setId(1L);
        matchedPerson.setMemberId(123);
        matchedPerson.setFirstName("John");
        matchedPerson.setLastName("Doe");
        matchedPerson.setEmail("old@example.com");
        matchedPerson.setActive(true);

        PersonRecord unmatchedPerson = PERSON.newRecord();
        unmatchedPerson.setId(2L);
        unmatchedPerson.setMemberId(456);
        unmatchedPerson.setFirstName("Jane");
        unmatchedPerson.setLastName("Smith");
        unmatchedPerson.setEmail("jane@example.com");
        unmatchedPerson.setActive(true);

        PersonRecord inactivePerson = PERSON.newRecord();
        inactivePerson.setId(3L);
        inactivePerson.setMemberId(789);
        inactivePerson.setFirstName("Bob");
        inactivePerson.setLastName("Wilson");
        inactivePerson.setEmail("bob@example.com");
        inactivePerson.setActive(false);

        when(personRepository.findAll(any())).thenReturn(List.of(matchedPerson, unmatchedPerson, inactivePerson));

        // When - Excel contains one update and one new person
        ExcelPersonData updateData = new ExcelPersonData(123, "John", "Doe", "new@example.com");
        ExcelPersonData newPersonData = new ExcelPersonData(999, "Alice", "Johnson", "alice@example.com");
        List<PersonChange> changes = detector.detectChanges(List.of(updateData, newPersonData));

        // Then - should have 1 UPDATE, 1 NEW, 1 DEACTIVATE
        assertThat(changes).hasSize(3);
        assertThat(changes).extracting(PersonChange::getType)
            .containsExactlyInAnyOrder(PersonChange.ChangeType.UPDATE, PersonChange.ChangeType.NEW,
                    PersonChange.ChangeType.DEACTIVATE);

        // Verify UPDATE
        PersonChange updateChange = changes.stream()
            .filter(c -> c.getType() == PersonChange.ChangeType.UPDATE)
            .findFirst()
            .orElseThrow();
        assertThat(updateChange.getExistingRecord()).isEqualTo(matchedPerson);

        // Verify NEW
        PersonChange newChange = changes.stream()
            .filter(c -> c.getType() == PersonChange.ChangeType.NEW)
            .findFirst()
            .orElseThrow();
        assertThat(newChange.getNewData()).isEqualTo(newPersonData);

        // Verify DEACTIVATE (only active unmatched person)
        PersonChange deactivateChange = changes.stream()
            .filter(c -> c.getType() == PersonChange.ChangeType.DEACTIVATE)
            .findFirst()
            .orElseThrow();
        assertThat(deactivateChange.getExistingRecord()).isEqualTo(unmatchedPerson);
    }

}
