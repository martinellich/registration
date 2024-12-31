package ch.martinelli.oss.registration.ui.views.events;

import ch.martinelli.oss.registration.db.tables.records.EventRecord;
import ch.martinelli.oss.registration.ui.views.KaribuTest;
import com.github.mvysny.kaributesting.v10.GridKt;
import com.github.mvysny.kaributesting.v10.NotificationsKt;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
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
    void add_person() {
        // Check the content of grid
        Grid<EventRecord> grid = _get(Grid.class);
        assertThat(GridKt._size(grid)).isEqualTo(3);
        assertThat(GridKt._get(grid, 0).getTitle()).isEqualTo("CIS 2024");

        // Add new person
        _setValue(_get(TextField.class, spec -> spec.withLabel("Bezeichnung")), "Jugendturntag");
        _setValue(_get(TextField.class, spec -> spec.withLabel("Ort")), "Kallnach");
        _setValue(_get(TextArea.class, spec -> spec.withLabel("Beschreibung")), "");
        _get(DatePicker.class, spec -> spec.withLabel("von")).setValue(LocalDate.of(2024, 6, 11));
        _get(DatePicker.class, spec -> spec.withLabel("bis")).setValue(LocalDate.of(2024, 6, 12));

        // Save
        _click(_get(Button.class, spec -> spec.withText("Speichern")));

        // Check if save was successful
        NotificationsKt.expectNotifications("Die Daten wurden gespeichert");
        assertThat(GridKt._size(grid)).isEqualTo(4);

        // Click new item and check value
        GridKt._clickItem(grid, 3);
        assertThat(_get(TextField.class, spec -> spec.withLabel("Bezeichnung")).getValue()).isEqualTo("Jugendturntag");
    }

    @Test
    void navigate_to_existing_event() {
        // Navigate to person with id 1
        UI.getCurrent().navigate(EventsView.class, new RouteParam(EventsView.EVENT_ID, "1"));

        // Check if the correct person is displayed
        assertThat(_get(TextField.class, spec -> spec.withLabel("Bezeichnung")).getValue()).isEqualTo("CIS 2024");
    }
}