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

        PersonRecord person2 = PERSON.newRecord();
        person2.setId(2L);
        person2.setMemberId(456);
        person2.setFirstName("Jane");
        person2.setLastName("Smith");
        person2.setEmail("jane@example.com");

        // Excel data matches person1 by member ID but has different name
        ExcelPersonData excelData = new ExcelPersonData(123, "Jane", "Smith", "updated@example.com");
        when(personRepository.findAll(any())).thenReturn(List.of(person1, person2));

        // When
        List<PersonChange> changes = detector.detectChanges(List.of(excelData));

        // Then - should match person1 by member ID (not person2 by name)
        assertThat(changes).hasSize(1);
        PersonChange change = changes.get(0);
        assertThat(change.getType()).isEqualTo(PersonChange.ChangeType.UPDATE);
        assertThat(change.getExistingRecord()).isEqualTo(person1);
    }

}
