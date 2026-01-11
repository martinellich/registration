package ch.martinelli.oss.registration.ui.views.registration;

import ch.martinelli.oss.registration.db.tables.records.EventRecord;
import ch.martinelli.oss.registration.db.tables.records.PersonRecord;
import ch.martinelli.oss.registration.db.tables.records.RegistrationViewRecord;
import ch.martinelli.oss.registration.security.Roles;
import ch.martinelli.oss.registration.ui.components.I18nDatePicker;
import ch.martinelli.oss.registration.ui.components.Icon;
import ch.martinelli.oss.registration.ui.views.KaribuTest;
import ch.martinelli.oss.testcontainers.mailpit.MailpitContainer;
import com.github.mvysny.kaributesting.v10.GridKt;
import com.github.mvysny.kaributesting.v10.LocatorJ;
import com.github.mvysny.kaributesting.v10.NotificationsKt;
import com.github.mvysny.kaributesting.v10.pro.ConfirmDialogKt;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouteParam;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Set;

import static ch.martinelli.oss.testcontainers.mailpit.assertions.MailpitAssertions.assertThat;
import static com.github.mvysny.kaributesting.v10.LocatorJ.*;

class RegistrationViewTest extends KaribuTest {

    @Autowired
    MailpitContainer mailpitContainer;

    @BeforeEach
    void login() {
        // Clear email container before each test
        mailpitContainer.getClient().deleteAllMessages();

        login("user@test.com", Roles.USER);
        UI.getCurrent().navigate(RegistrationView.class);
    }

    @Test
    void add_registration_create_mailing_send_emails() {
        // Enable showing past invitations to see all test data
        _click(_get(Button.class, spec -> spec.withId("toggle-past-invitations-button")));

        // Check the content of grid
        @SuppressWarnings("unchecked")
        var grid = (Grid<RegistrationViewRecord>) _get(Grid.class);
        Assertions.assertThat(GridKt._size(grid)).isEqualTo(3);
        Assertions.assertThat(GridKt._get(grid, 0).getYear()).isEqualTo(2025);
        Assertions.assertThat(GridKt._get(grid, 1).getYear()).isEqualTo(2024);
        Assertions.assertThat(GridKt._get(grid, 2).getYear()).isEqualTo(2023);

        // Add new registration
        _click(_get(Icon.class, spec -> spec.withId("add-icon")));

        _setValue(_get(TextField.class, spec -> spec.withLabel("Bezeichnung")), "Jugi TV Erlach - Anmeldung");
        _setValue(_get(IntegerField.class, spec -> spec.withLabel("Jahr")), 2025);
        _get(I18nDatePicker.class, spec -> spec.withLabel("Offen von")).setValue(LocalDate.of(2025, 1, 1));
        _get(I18nDatePicker.class, spec -> spec.withLabel("Offen bis")).setValue(LocalDate.of(2025, 2, 28));

        @SuppressWarnings("unchecked")
        var eventListBox = (MultiSelectListBox<EventRecord>) _get(MultiSelectListBox.class,
                spec -> spec.withId("event-list-box"));
        eventListBox.setValue(Set.of(eventListBox.getListDataView().getItem(0)));
        @SuppressWarnings("unchecked")
        var personListBox = (MultiSelectListBox<PersonRecord>) _get(MultiSelectListBox.class,
                spec -> spec.withId("person-list-box"));
        // Find Lettie Bennett by email instead of relying on index order
        var lettieBennett = personListBox.getListDataView()
            .getItems()
            .filter(p -> "lettie.bennett@odeter.bb".equals(p.getEmail()))
            .findFirst()
            .orElseThrow();
        personListBox.setValue(Set.of(lettieBennett));

        _click(_get(Button.class, spec -> spec.withText("Speichern")));

        NotificationsKt.expectNotifications("Der Datensatz wurden gespeichert");

        Assertions.assertThat(GridKt._size(grid)).isEqualTo(4);

        // Select newly created record (at index 1 because it's sorted by year desc, then
        // title asc)
        var registrationViewRecord = GridKt._get(grid, 1);
        Assertions.assertThat(registrationViewRecord.getTitle()).isEqualTo("Jugi TV Erlach - Anmeldung");

        // Create mailing
        _click(_get(Button.class, spec -> spec.withText("Versand erstellen")));

        // Confirm
        ConfirmDialogKt._fireConfirm(_get(ConfirmDialog.class));

        // Check if save was successful
        NotificationsKt.expectNotifications("Der Versand wurde erstellt");

        // Create mailing
        _click(_get(Button.class, spec -> spec.withText("E-Mails verschicken")));

        // Confirm
        ConfirmDialogKt._fireConfirm(_get(ConfirmDialog.class));

        // Check if save was successful
        NotificationsKt.expectNotifications("Die E-Mails werden nun versendet");

        assertThat(mailpitContainer).withTimeout(Duration.ofSeconds(30))
            .awaitMessageCount(1)
            .firstMessage()
            .hasSubject("Jugi TV Erlach - Anmeldung 2025")
            .hasRecipient("lettie.bennett@odeter.bb");

        // Delete new item (at index 1 where we created it)
        GridKt._getCellComponent(grid, 1, "action-column")
            .getChildren()
            .filter(child -> child.getId().isPresent() && child.getId().get().equals("delete-action"))
            .findFirst()
            .map(Icon.class::cast)
            .ifPresent(LocatorJ::_click);

        ConfirmDialogKt._fireConfirm(_get(ConfirmDialog.class));

        NotificationsKt.expectNotifications("Der Datensatz wurde gelöscht");
    }

    @Test
    void navigate_to_non_existing_registration() {
        // 999 doesn't exist
        UI.getCurrent().navigate(RegistrationView.class, new RouteParam(RegistrationView.ID, "9999"));

        // Form must be empty
        Assertions.assertThat(_get(IntegerField.class, spec -> spec.withLabel("Jahr")).getValue()).isNull();
    }

}