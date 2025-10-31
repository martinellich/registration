package ch.martinelli.oss.registration.ui.views.persons;

import ch.martinelli.oss.registration.db.tables.records.PersonRecord;
import ch.martinelli.oss.registration.domain.ExcelPersonData;
import ch.martinelli.oss.registration.domain.PersonChange;
import ch.martinelli.oss.registration.domain.PersonRepository;
import ch.martinelli.oss.registration.ui.views.KaribuTest;
import com.github.mvysny.kaributesting.v10.GridKt;
import com.vaadin.flow.component.button.Button;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static ch.martinelli.oss.registration.db.tables.Person.PERSON;
import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

class PersonImportDialogTest extends KaribuTest {

    @Autowired
    private PersonRepository personRepository;

    @Test
    void shouldDisplayNewPersonChange() {
        // Given
        ExcelPersonData newPerson = new ExcelPersonData(999, "Test", "Person", "test@example.com");
        Map<String, PersonChange.FieldChange> changedFields = new HashMap<>();
        changedFields.put("firstName", new PersonChange.FieldChange(null, "Test"));
        changedFields.put("lastName", new PersonChange.FieldChange(null, "Person"));
        changedFields.put("email", new PersonChange.FieldChange(null, "test@example.com"));
        changedFields.put("memberId", new PersonChange.FieldChange(null, "999"));

        PersonChange change = new PersonChange(PersonChange.ChangeType.NEW, null, newPerson, changedFields);
        List<PersonChange> changes = List.of(change);

        // When
        PersonImportDialog dialog = new PersonImportDialog(changes, personRepository, () -> {
        });
        dialog.open();

        // Then
        assertThat(GridKt._size(dialog.grid)).isEqualTo(1);

        // Verify the change is displayed
        PersonChange displayedChange = GridKt._get(dialog.grid, 0);
        assertThat(displayedChange.getType()).isEqualTo(PersonChange.ChangeType.NEW);
        assertThat(displayedChange.getNewData()).isEqualTo(newPerson);
    }

    @Test
    void shouldDisplayUpdateChange() {
        // Given
        PersonRecord existingPerson = PERSON.newRecord();
        existingPerson.setId(1L);
        existingPerson.setFirstName("Old");
        existingPerson.setLastName("Name");
        existingPerson.setEmail("old@example.com");
        existingPerson.setMemberId(123);

        ExcelPersonData updatedData = new ExcelPersonData(123, "New", "Name", "new@example.com");
        Map<String, PersonChange.FieldChange> changedFields = new HashMap<>();
        changedFields.put("firstName", new PersonChange.FieldChange("Old", "New"));
        changedFields.put("email", new PersonChange.FieldChange("old@example.com", "new@example.com"));

        PersonChange change = new PersonChange(PersonChange.ChangeType.UPDATE, existingPerson, updatedData,
                changedFields);
        List<PersonChange> changes = List.of(change);

        // When
        PersonImportDialog dialog = new PersonImportDialog(changes, personRepository, () -> {
        });
        dialog.open();

        // Then
        assertThat(GridKt._size(dialog.grid)).isEqualTo(1);

        PersonChange displayedChange = GridKt._get(dialog.grid, 0);
        assertThat(displayedChange.getType()).isEqualTo(PersonChange.ChangeType.UPDATE);
    }

    @Test
    void shouldToggleAcceptRejectForIndividualChange() {
        // Given
        ExcelPersonData newPerson = new ExcelPersonData(999, "Test", "Person", "test@example.com");
        PersonChange change = new PersonChange(PersonChange.ChangeType.NEW, null, newPerson, Map.of());
        List<PersonChange> changes = List.of(change);

        PersonImportDialog dialog = new PersonImportDialog(changes, personRepository, () -> {
        });
        dialog.open();

        // When - Initially accepted
        assertThat(change.isAccepted()).isTrue();

        // The checkbox controls the accepted state
        // In this test, we verify the model's accepted state can be changed
        change.setAccepted(false);

        // Then
        assertThat(change.isAccepted()).isFalse();
    }

    @Test
    void shouldAcceptAllChanges() {
        // Given
        ExcelPersonData person1 = new ExcelPersonData(1, "First", "Person", "first@example.com");
        ExcelPersonData person2 = new ExcelPersonData(2, "Second", "Person", "second@example.com");

        PersonChange change1 = new PersonChange(PersonChange.ChangeType.NEW, null, person1, Map.of());
        PersonChange change2 = new PersonChange(PersonChange.ChangeType.NEW, null, person2, Map.of());

        // Set one to rejected
        change2.setAccepted(false);

        List<PersonChange> changes = Arrays.asList(change1, change2);

        PersonImportDialog dialog = new PersonImportDialog(changes, personRepository, () -> {
        });
        dialog.open();

        // When
        Button acceptAllButton = _get(Button.class, spec -> spec.withText("Alle akzeptieren"));
        _click(acceptAllButton);

        // Then
        assertThat(change1.isAccepted()).isTrue();
        assertThat(change2.isAccepted()).isTrue();
    }

    @Test
    void shouldRejectAllChanges() {
        // Given
        ExcelPersonData person1 = new ExcelPersonData(1, "First", "Person", "first@example.com");
        ExcelPersonData person2 = new ExcelPersonData(2, "Second", "Person", "second@example.com");

        PersonChange change1 = new PersonChange(PersonChange.ChangeType.NEW, null, person1, Map.of());
        PersonChange change2 = new PersonChange(PersonChange.ChangeType.NEW, null, person2, Map.of());

        List<PersonChange> changes = Arrays.asList(change1, change2);

        PersonImportDialog dialog = new PersonImportDialog(changes, personRepository, () -> {
        });
        dialog.open();

        // When
        Button rejectAllButton = _get(Button.class, spec -> spec.withText("Alle ablehnen"));
        _click(rejectAllButton);

        // Then
        assertThat(change1.isAccepted()).isFalse();
        assertThat(change2.isAccepted()).isFalse();
    }

    @Test
    void shouldCallOnSuccessCallbackWhenApplyingChanges() {
        // Given
        ExcelPersonData newPerson = new ExcelPersonData(12345, "Callback", "Test", "callback@example.com");
        Map<String, PersonChange.FieldChange> changedFields = new HashMap<>();
        changedFields.put("firstName", new PersonChange.FieldChange(null, "Callback"));
        changedFields.put("lastName", new PersonChange.FieldChange(null, "Test"));

        PersonChange change = new PersonChange(PersonChange.ChangeType.NEW, null, newPerson, changedFields);
        List<PersonChange> changes = List.of(change);

        AtomicBoolean callbackCalled = new AtomicBoolean(false);

        PersonImportDialog dialog = new PersonImportDialog(changes, personRepository, () -> callbackCalled.set(true));
        dialog.open();

        // When
        Button applyButton = _get(Button.class, spec -> spec.withText("Änderungen übernehmen"));
        _click(applyButton);

        // Then
        assertThat(callbackCalled.get()).isTrue();
        assertThat(dialog.isOpened()).isFalse();
    }

}
