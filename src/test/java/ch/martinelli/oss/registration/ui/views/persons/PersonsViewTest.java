package ch.martinelli.oss.registration.ui.views.persons;

import ch.martinelli.oss.registration.db.tables.records.EventRecord;
import ch.martinelli.oss.registration.db.tables.records.PersonRecord;
import ch.martinelli.oss.registration.ui.components.I18nDatePicker;
import ch.martinelli.oss.registration.ui.components.Icon;
import ch.martinelli.oss.registration.ui.views.KaribuTest;
import com.github.mvysny.kaributesting.v10.GridKt;
import com.github.mvysny.kaributesting.v10.NotificationsKt;
import com.github.mvysny.kaributesting.v10.pro.ConfirmDialogKt;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouteParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.github.mvysny.kaributesting.v10.LocatorJ.*;
import static org.assertj.core.api.Assertions.assertThat;

class PersonsViewTest extends KaribuTest {

    @BeforeEach
    void login() {
        login("simon@martinelli.ch");
        UI.getCurrent().getPage().reload();

        UI.getCurrent().navigate(PersonsView.class);
    }

    @Test
    void add_person() {
        // Check the content of grid
        @SuppressWarnings("unchecked")
        Grid<PersonRecord> grid = _get(Grid.class);
        assertThat(GridKt._size(grid)).isEqualTo(12);
        assertThat(GridKt._get(grid, 0).getFirstName()).isEqualTo("Lettie");

        // Add new person
        _click(_get(Icon.class, spec -> spec.withId("add-icon")));

        _setValue(_get(TextField.class, spec -> spec.withLabel("Nachname")), "Martinelli");
        _setValue(_get(TextField.class, spec -> spec.withLabel("Vorname")), "Simon");
        _setValue(_get(EmailField.class, spec -> spec.withLabel("E-Mail")), "simon@tverlach.ch");
        _get(I18nDatePicker.class, spec -> spec.withLabel("Geburtsdatum")).setValue(LocalDate.of(2010, 1, 12));
        _get(Checkbox.class, spec -> spec.withLabel("Aktiv")).setValue(true);

        // Save
        _click(_get(Button.class, spec -> spec.withText("Speichern")));

        // Check if save was successful
        NotificationsKt.expectNotifications("Der Datensatz wurden gespeichert");
        assertThat(GridKt._size(grid)).isEqualTo(13);

        // Click new item and check value
        GridKt._clickItem(grid, 6);
        assertThat(_get(TextField.class, spec -> spec.withLabel("Nachname")).getValue()).isEqualTo("Martinelli");

        // Delete new item
        Component component = GridKt._getCellComponent(grid, 6, "action-column");
        if (component instanceof Icon icon) {
            _click(icon);
        }

        ConfirmDialogKt._fireConfirm(_get(ConfirmDialog.class));

        NotificationsKt.expectNotifications("Der Datensatz wurde gelöscht");
    }

    @Test
    void navigate_to_existing_person() {
        // Navigate to person with id 1
        UI.getCurrent().navigate(PersonsView.class, new RouteParam(PersonsView.ID, "1"));

        // Check if the correct person is displayed
        assertThat(_get(TextField.class, spec -> spec.withLabel("Nachname")).getValue()).isEqualTo("Lane");

        // Cancel edit
        _click(_get(Button.class, spec -> spec.withText("Abbrechen")));
        assertThat(_get(TextField.class, spec -> spec.withLabel("Nachname")).isEnabled()).isFalse();
    }

    @Test
    void navigate_to_non_existing_person() {
        // Navigate to person with id 1
        UI.getCurrent().navigate(PersonsView.class, new RouteParam(PersonsView.ID, "999"));

        // Check if the no person is displayed
        assertThat(_get(TextField.class, spec -> spec.withLabel("Nachname")).getValue()).isEmpty();
    }

    @Test
    void try_to_delete_used_person() {
        @SuppressWarnings("unchecked")
        Grid<EventRecord> grid = _get(Grid.class);
        Component component = GridKt._getCellComponent(grid, 4, "action-column");
        if (component instanceof Icon icon) {
            _click(icon);
        }

        ConfirmDialogKt._fireConfirm(_get(ConfirmDialog.class));

        NotificationsKt.expectNotifications("Der Datensatz wird noch verwendet und kann nicht gelöscht werden");
    }

}