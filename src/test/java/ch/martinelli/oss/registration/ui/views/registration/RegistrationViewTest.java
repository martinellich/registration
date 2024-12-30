package ch.martinelli.oss.registration.ui.views.registration;

import ch.martinelli.oss.registration.db.tables.records.RegistrationRecord;
import ch.martinelli.oss.registration.ui.views.KaribuTest;
import com.github.mvysny.kaributesting.v10.GridKt;
import com.github.mvysny.kaributesting.v10.NotificationsKt;
import com.github.mvysny.kaributesting.v10.pro.ConfirmDialogKt;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.IntegerField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.github.mvysny.kaributesting.v10.LocatorJ.*;
import static org.assertj.core.api.Assertions.assertThat;

class RegistrationViewTest extends KaribuTest {

    @BeforeEach
    void login() {
        login("simon@martinelli.ch");
        UI.getCurrent().getPage().reload();

        UI.getCurrent().navigate(RegistrationView.class);
    }

    @Test
    void add_registration_and_create_mailing() {
        // Check the content of grid
        Grid<RegistrationRecord> grid = _get(Grid.class);
        assertThat(GridKt._size(grid)).isEqualTo(2);
        assertThat(GridKt._get(grid, 0).getYear()).isEqualTo(2023);
        assertThat(GridKt._get(grid, 1).getYear()).isEqualTo(2024);

        // Add new person
        _setValue(_get(IntegerField.class, spec -> spec.withLabel("Jahr")), 2026);
        _get(DatePicker.class, spec -> spec.withLabel("Offen von")).setValue(LocalDate.of(2026, 1, 1));
        _get(DatePicker.class, spec -> spec.withLabel("Offen bis")).setValue(LocalDate.of(2026, 2, 28));

        _click(_get(Button.class, spec -> spec.withText("Speichern")));

        NotificationsKt.expectNotifications("Die Daten wurden gespeichert");

        assertThat(GridKt._size(grid)).isEqualTo(3);

        // Select newly created record
        GridKt._clickItem(grid, 2);

        // Create mailing
        _click(_get(Button.class, spec -> spec.withText("Versand erstellen")));

        // Confirm
        ConfirmDialogKt._fireConfirm(_get(ConfirmDialog.class));

        // Check if save was successful
        NotificationsKt.expectNotifications("Der Versand wurde erstellt");
    }

}