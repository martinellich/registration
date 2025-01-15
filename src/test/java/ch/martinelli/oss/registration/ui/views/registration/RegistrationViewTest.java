package ch.martinelli.oss.registration.ui.views.registration;

import ch.martinelli.oss.registration.db.tables.records.EventRecord;
import ch.martinelli.oss.registration.db.tables.records.PersonRecord;
import ch.martinelli.oss.registration.db.tables.records.RegistrationViewRecord;
import ch.martinelli.oss.registration.ui.components.I18nDatePicker;
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
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouteParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import skydrinker.testcontainers.mailcatcher.MailCatcherContainer;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.github.mvysny.kaributesting.v10.LocatorJ.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class RegistrationViewTest extends KaribuTest {

    @Container
    static final MailCatcherContainer mailcatcherContainer = new MailCatcherContainer();

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        mailcatcherContainer.start();

        registry.add("spring.mail.host", mailcatcherContainer::getHost);
        registry.add("spring.mail.port", mailcatcherContainer::getSmtpPort);
        registry.add("spring.mail.username", () -> "jugi@tverlach.ch");
        registry.add("spring.mail.password", () -> "pass");
    }

    @BeforeEach
    void login() {
        login("simon@martinelli.ch");
        UI.getCurrent().getPage().reload();

        UI.getCurrent().navigate(RegistrationView.class);
    }

    @Test
    void add_registration_create_mailing_send_emails() {
        // Check the content of grid
        @SuppressWarnings("unchecked")
        Grid<RegistrationViewRecord> grid = _get(Grid.class);
        assertThat(GridKt._size(grid)).isEqualTo(2);
        assertThat(GridKt._get(grid, 0).getYear()).isEqualTo(2024);
        assertThat(GridKt._get(grid, 1).getYear()).isEqualTo(2023);

        // Add new registration
        _click(_get(Icon.class, spec -> spec.withId("add-icon")));

        _setValue(_get(TextField.class, spec -> spec.withLabel("Bezeichnung")), "Jugi TV Erlach - Anmeldung");
        _setValue(_get(IntegerField.class, spec -> spec.withLabel("Jahr")), 2025);
        _get(I18nDatePicker.class, spec -> spec.withLabel("Offen von")).setValue(LocalDate.of(2025, 1, 1));
        _get(I18nDatePicker.class, spec -> spec.withLabel("Offen bis")).setValue(LocalDate.of(2025, 2, 28));

        @SuppressWarnings("unchecked")
        MultiSelectListBox<EventRecord> eventListBox = _get(MultiSelectListBox.class,
                spec -> spec.withId("event-list-box"));
        eventListBox.setValue(Set.of(eventListBox.getListDataView().getItem(0)));
        @SuppressWarnings("unchecked")
        MultiSelectListBox<PersonRecord> personListBox = _get(MultiSelectListBox.class,
                spec -> spec.withId("person-list-box"));
        personListBox.setValue(Set.of(personListBox.getListDataView().getItem(0)));

        _click(_get(Button.class, spec -> spec.withText("Speichern")));

        NotificationsKt.expectNotifications("Der Datensatz wurden gespeichert");

        assertThat(GridKt._size(grid)).isEqualTo(3);

        // Select newly created record
        RegistrationViewRecord registrationViewRecord = GridKt._get(grid, 0);
        assertThat(registrationViewRecord.getTitle()).isEqualTo("Jugi TV Erlach - Anmeldung");

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

        await().atLeast(2, TimeUnit.SECONDS);

        assertThat(mailcatcherContainer.getAllEmails()).hasSize(1).first().satisfies(mail -> {
            assertThat(mail.getSubject()).isEqualTo("Jugi TV Erlach - Anmeldung 2025");
            assertThat(mail.getRecipients()).first().isEqualTo("<lettie.bennett@odeter.bb>");
        });

        // Delete new item
        GridKt._getCellComponent(grid, 2, "action-column")
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
        assertThat(_get(IntegerField.class, spec -> spec.withLabel("Jahr")).getValue()).isNull();
    }

}