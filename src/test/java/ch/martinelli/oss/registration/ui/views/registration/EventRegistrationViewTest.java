package ch.martinelli.oss.registration.ui.views.registration;

import ch.martinelli.oss.registration.domain.EventRegistrationRow;
import ch.martinelli.oss.registration.ui.views.KaribuTest;
import com.github.mvysny.kaributesting.v10.GridKt;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

class EventRegistrationViewTest extends KaribuTest {

    @BeforeEach
    void login() {
        login("simon@martinelli.ch");
        UI.getCurrent().getPage().reload();

        UI.getCurrent().navigate(EventRegistrationView.class, 1L);
    }

    @Test
    void display_event_registrations() {
        // Check the content of grid
        Grid<EventRegistrationRow> grid = _get(Grid.class);

        assertThat(GridKt._size(grid)).isEqualTo(2);
        assertThat(GridKt._get(grid, 0).firstName()).isEqualTo("Eula");
    }

}