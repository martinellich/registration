package ch.martinelli.oss.registration.ui.views.registration;

import ch.martinelli.oss.registration.db.tables.records.EventRegistrationViewRecord;
import ch.martinelli.oss.registration.ui.views.KaribuTest;
import com.github.mvysny.kaributesting.v10.GridKt;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.mvysny.kaributesting.v10.LocatorJ.*;
import static org.assertj.core.api.Assertions.assertThat;

class EventRegistrationViewTest extends KaribuTest {

    @BeforeEach
    void login() {
        login("simon@martinelli.ch");
        UI.getCurrent().getPage().reload();

        UI.getCurrent().navigate(EventRegistrationView.class);
    }

    @Test
    void display_event_registrations() {
        // Check the content of grid
        Grid<EventRegistrationViewRecord> grid = _get(Grid.class);
        assertThat(GridKt._size(grid)).isEqualTo(1);
        assertThat(GridKt._get(grid, 0).getFirstName()).isEqualTo("Eula");

        // Filter
        _setValue(_get(TextField.class, spec -> spec.withPlaceholder("Vor- oder Nachname")), "Simon");
        _click(_get(Button.class, spec -> spec.withText("Suchen")));

        // Check the content of grid
        assertThat(GridKt._size(grid)).isZero();

        // Filter
        _setValue(_get(TextField.class, spec -> spec.withPlaceholder("Vor- oder Nachname")), "Eula");
        _click(_get(Button.class, spec -> spec.withText("Suchen")));

        // Check the content of grid
        assertThat(GridKt._size(grid)).isEqualTo(1);

        // Clear Filter
        _click(_get(Button.class, spec -> spec.withText("Reset")));

        // Check the content of grid
        assertThat(GridKt._size(grid)).isEqualTo(1);
    }

}