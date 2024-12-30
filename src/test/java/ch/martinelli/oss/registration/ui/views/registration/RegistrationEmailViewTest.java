package ch.martinelli.oss.registration.ui.views.registration;

import ch.martinelli.oss.registration.db.tables.records.RegistrationEmailViewRecord;
import ch.martinelli.oss.registration.ui.views.KaribuTest;
import com.github.mvysny.kaributesting.v10.GridKt;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.IntegerField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.mvysny.kaributesting.v10.LocatorJ.*;
import static org.assertj.core.api.Assertions.assertThat;

class RegistrationEmailViewTest extends KaribuTest {
    @BeforeEach
    void login() {
        login("simon@martinelli.ch", "", List.of("ADMIN"));
        UI.getCurrent().getPage().reload();

        UI.getCurrent().navigate(RegistrationEmailView.class);
    }

    @Test
    void display_registration_emails() {
        // Check the content of grid
        Grid<RegistrationEmailViewRecord> grid = _get(Grid.class);
        assertThat(GridKt._size(grid)).isEqualTo(1);
        assertThat(GridKt._get(grid, 0).getYear()).isEqualTo(2023);

        // Filter
        _setValue(_get(IntegerField.class, spec -> spec.withPlaceholder("Jahr")), 2024);
        _click(_get(Button.class, spec -> spec.withText("Suchen")));

        // Check the content of grid
        assertThat(GridKt._size(grid)).isZero();

        // Filter
        _setValue(_get(IntegerField.class, spec -> spec.withPlaceholder("Jahr")), 2023);
        _click(_get(Button.class, spec -> spec.withText("Suchen")));

        // Check the content of grid
        assertThat(GridKt._size(grid)).isEqualTo(1);

        // Clear Filter
        _click(_get(Button.class, spec -> spec.withText("Reset")));

        // Check the content of grid
        assertThat(GridKt._size(grid)).isEqualTo(1);
    }
}