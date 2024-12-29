package ch.martinelli.oss.registration.ui.views.persons;

import ch.martinelli.oss.registration.db.tables.records.PersonRecord;
import ch.martinelli.oss.registration.ui.views.KaribuTest;
import com.github.mvysny.kaributesting.v10.GridKt;
import com.github.mvysny.kaributesting.v10.NotificationsKt;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.github.mvysny.kaributesting.v10.LocatorJ.*;
import static org.assertj.core.api.Assertions.assertThat;

class PersonsViewTest extends KaribuTest {

    @BeforeEach
    void login() {
        login("simon@martinelli.ch", "", List.of("ADMIN"));
        UI.getCurrent().getPage().reload();

        UI.getCurrent().navigate(PersonsView.class);
    }

    @Test
    void add_person() {
        // Check the content of grid
        Grid<PersonRecord> grid = _get(Grid.class);
        assertThat(GridKt._size(grid)).isEqualTo(12);
        assertThat(GridKt._get(grid, 0).getFirstName()).isEqualTo("Eula");

        // Add new person
        _setValue(_get(TextField.class, spec -> spec.withLabel("Nachname")), "Martinelli");
        _setValue(_get(TextField.class, spec -> spec.withLabel("Vorname")), "Simon");
        _setValue(_get(EmailField.class, spec -> spec.withLabel("Email")), "simon@tverlach.ch");
        _get(DatePicker.class, spec -> spec.withLabel("Geburtsdatum")).setValue(LocalDate.of(2010, 1, 12));
        _get(Checkbox.class, spec -> spec.withLabel("Aktiv")).setValue(true);

        // Save
        _click(_get(Button.class, spec -> spec.withText("Speichern")));

        // Check if save was successful
        NotificationsKt.expectNotifications("Die Daten wurden gespeichert");
        assertThat(GridKt._size(grid)).isEqualTo(13);
    }

}