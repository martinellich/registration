package ch.martinelli.oss.registration.ui.views.events;

import ch.martinelli.oss.registration.db.tables.records.EventRecord;
import ch.martinelli.oss.registration.ui.components.I18nDatePicker;
import ch.martinelli.oss.registration.ui.components.Icon;
import ch.martinelli.oss.registration.ui.views.KaribuTest;
import com.github.mvysny.kaributesting.v10.GridKt;
import com.github.mvysny.kaributesting.v10.NotificationsKt;
import com.github.mvysny.kaributesting.v10.pro.ConfirmDialogKt;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouteParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.github.mvysny.kaributesting.v10.LocatorJ.*;
import static org.assertj.core.api.Assertions.assertThat;

class EventsViewTest extends KaribuTest {

    @BeforeEach
    void login() {
        login("simon@martinelli.ch");
        UI.getCurrent().getPage().reload();

        UI.getCurrent().navigate(EventsView.class);
    }

    @Test
    void add_and_delete_event() {
        // Check the content of grid
        @SuppressWarnings("unchecked")
        Grid<EventRecord> grid = _get(Grid.class);
        assertThat(GridKt._size(grid)).isEqualTo(5);
        assertThat(GridKt._get(grid, 0).getTitle()).isEqualTo("CIS 2023");

        // Add new person
        _click(_get(Icon.class, spec -> spec.withId("add-icon")));

        _setValue(_get(TextField.class, spec -> spec.withLabel("Bezeichnung")), "Jugendturntag");
        _setValue(_get(TextField.class, spec -> spec.withLabel("Ort")), "Kallnach");
        _setValue(_get(TextArea.class, spec -> spec.withLabel("Beschreibung")), "");
        _get(I18nDatePicker.class, spec -> spec.withLabel("von")).setValue(LocalDate.of(2024, 6, 11));
        _get(I18nDatePicker.class, spec -> spec.withLabel("bis")).setValue(LocalDate.of(2024, 6, 12));

        // Save
        _click(_get(Button.class, spec -> spec.withText("Speichern")));

        // Check if save was successful
        NotificationsKt.expectNotifications("Der Datensatz wurden gespeichert");
        assertThat(GridKt._size(grid)).isEqualTo(6);

        // Click new item and check value
        GridKt._clickItem(grid, 5);
        assertThat(_get(TextField.class, spec -> spec.withLabel("Bezeichnung")).getValue()).isEqualTo("Jugendturntag");

        // Delete new item
        Component component = GridKt._getCellComponent(grid, 5, "action-column");
        if (component instanceof Icon icon) {
            _click(icon);
        }

        ConfirmDialogKt._fireConfirm(_get(ConfirmDialog.class));

        NotificationsKt.expectNotifications("Der Datensatz wurde gelöscht");
    }

    @Test
    void navigate_to_existing_event() {
        // Navigate to event with id 1
        UI.getCurrent().navigate(EventsView.class, new RouteParam(EventsView.ID, "1"));

        // Check if the correct event is displayed
        assertThat(_get(TextField.class, spec -> spec.withLabel("Bezeichnung")).getValue()).isEqualTo("CIS 2023");


        // Cancel edit
        _click(_get(Button.class, spec -> spec.withText("Abbrechen")));
        assertThat(_get(TextField.class, spec -> spec.withLabel("Bezeichnung")).isEnabled()).isFalse();
    }

    @Test
    void navigate_to_non_existing_event() {
        // Navigate to event with id 1
        UI.getCurrent().navigate(EventsView.class, new RouteParam(EventsView.ID, "9999"));

        // Check if the no person is displayed
        assertThat(_get(TextField.class, spec -> spec.withLabel("Bezeichnung")).getValue()).isEmpty();
    }

    @Test
    void try_to_delete_used_event() {
        @SuppressWarnings("unchecked")
        Grid<EventRecord> grid = _get(Grid.class);
        Component component = GridKt._getCellComponent(grid, 0, "action-column");
        if (component instanceof Icon icon) {
            _click(icon);
        }

        ConfirmDialogKt._fireConfirm(_get(ConfirmDialog.class));

        NotificationsKt.expectNotifications("Der Datensatz wird noch verwendet und kann nicht gelöscht werden");
    }

}