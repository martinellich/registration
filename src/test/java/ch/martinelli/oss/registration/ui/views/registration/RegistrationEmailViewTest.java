package ch.martinelli.oss.registration.ui.views.registration;

import ch.martinelli.oss.registration.db.tables.records.RegistrationEmailViewRecord;
import ch.martinelli.oss.registration.ui.components.Icon;
import ch.martinelli.oss.registration.ui.views.KaribuTest;
import com.github.mvysny.kaributesting.v10.GridKt;
import com.github.mvysny.kaributesting.v10.LocatorJ;
import com.github.mvysny.kaributesting.v10.NotificationsKt;
import com.github.mvysny.kaributesting.v10.pro.ConfirmDialogKt;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.select.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.mvysny.kaributesting.v10.LocatorJ.*;
import static org.assertj.core.api.Assertions.assertThat;

class RegistrationEmailViewTest extends KaribuTest {

    @BeforeEach
    void login() {
        UI.getCurrent().navigate(RegistrationEmailView.class);
    }

    @Test
    void display_registration_emails() {
        // Check the content of grid
        @SuppressWarnings("unchecked")
        Grid<RegistrationEmailViewRecord> grid = _get(Grid.class);
        assertThat(GridKt._size(grid)).isZero();

        // Filter
        @SuppressWarnings("rawtypes")
        Select select = _get(Select.class);
        // noinspection unchecked
        _setValue(_get(Select.class), select.getListDataView().getItem(1));

        assertThat(GridKt._size(grid)).isNotZero();
        assertThat(GridKt._get(grid, 0).getYear()).isEqualTo(2023);

        // Filter
        // noinspection unchecked
        _setValue(_get(Select.class), select.getListDataView().getItem(0));

        // Check the content of grid
        assertThat(GridKt._size(grid)).isZero();

        // Clear Filter
        _click(_get(Button.class, spec -> spec.withText("Reset")));

        // Check the content of grid
        assertThat(GridKt._size(grid)).isZero();
    }

    @Test
    void delete_registration_email() {
        // Check the content of grid
        @SuppressWarnings("unchecked")
        Grid<RegistrationEmailViewRecord> grid = _get(Grid.class);
        assertThat(GridKt._size(grid)).isZero();

        // Filter
        @SuppressWarnings("rawtypes")
        Select select = _get(Select.class);
        // noinspection unchecked
        _setValue(_get(Select.class), select.getListDataView().getItem(1));

        assertThat(GridKt._size(grid)).isNotZero();
        assertThat(GridKt._get(grid, 0).getYear()).isEqualTo(2023);

        // Delete new item
        GridKt._getCellComponent(grid, 1, "action-column")
            .getChildren()
            .filter(child -> child.getId().isPresent() && child.getId().get().equals("delete-action"))
            .findFirst()
            .map(Icon.class::cast)
            .ifPresent(LocatorJ::_click);

        ConfirmDialogKt._fireConfirm(_get(ConfirmDialog.class));

        NotificationsKt.expectNotifications("Der Datensatz wurde gelöscht");
    }

}