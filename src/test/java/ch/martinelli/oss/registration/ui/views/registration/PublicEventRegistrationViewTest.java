package ch.martinelli.oss.registration.ui.views.registration;

import ch.martinelli.oss.registration.ui.views.KaribuTest;
import com.github.mvysny.kaributesting.v10.LocatorJ;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.ListItem;
import org.junit.jupiter.api.Test;

import static com.github.mvysny.kaributesting.v10.LocatorJ.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class PublicEventRegistrationViewTest extends KaribuTest {

    @Test
    void navigation_without_parameter() {
        UI ui = UI.getCurrent();
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> ui.navigate(PublicEventRegistrationView.class))
            .withMessage(
                    "Navigation target 'ch.martinelli.oss.registration.ui.views.registration.PublicEventRegistrationView' requires a parameter.");
    }

    @Test
    void navigation_with_correct_parameter() {
        // Use registration ID 3 with open_until='2099-12-31' (open registration)
        UI.getCurrent().navigate("public/openregistrationlink123456789");

        var title = _get(ListItem.class, spec -> spec.withText("Rodriquez Barry"));
        assertThat(title).isNotNull();

        var checkboxes = LocatorJ._find(Checkbox.class);
        // Note: Event 4 is mandatory (first checkbox), so it's already checked and
        // disabled
        // Only click on enabled checkboxes
        checkboxes.stream().filter(Checkbox::isEnabled).forEach(checkbox -> LocatorJ._click(checkbox));

        // Button text is "Anmeldung aktualisieren" because there are existing
        // registrations
        _click(_get(Button.class, spec -> spec.withText("Anmeldung aktualisieren")));

        var button = _get(Button.class);
        assertThat(button.getText()).isEqualTo("Die Antwort wurde aktualisiert");
    }

    @Test
    void registration_closed_after_cutoff_date() {
        // Test data uses registration ID 1 with open_until='2023-02-28', which is in the
        // past
        UI.getCurrent().navigate("public/550e8400e29b41d4a716446655440000");

        // Verify person is displayed
        var person = _get(ListItem.class, spec -> spec.withText("Lane Eula"));
        assertThat(person).isNotNull();

        // Verify notification banner is displayed
        var closedNotifications = _find(Div.class, spec -> spec.withText(
                "Die Anmeldefrist ist abgelaufen. Sie können Ihre Auswahl ansehen, aber keine Änderungen vornehmen."));
        assertThat(closedNotifications).hasSize(1);

        // Verify all checkboxes are disabled
        var checkboxes = _find(Checkbox.class);
        assertThat(checkboxes).isNotEmpty().allMatch(checkbox -> !checkbox.isEnabled());

        // Verify the submit button is disabled
        var button = _get(Button.class, spec -> spec.withText("Anmeldung aktualisieren"));
        assertThat(button.isEnabled()).isFalse();
    }

    @Test
    void registration_open_before_cutoff_date() {
        // Test data uses registration ID 3 with open_until='2099-12-31', which is in the
        // future
        UI.getCurrent().navigate("public/openregistrationlink123456789");

        // Verify person is displayed
        var person = _get(ListItem.class, spec -> spec.withText("Rodriquez Barry"));
        assertThat(person).isNotNull();

        // Verify NO notification banner is displayed
        var closedNotifications = _find(Div.class, spec -> spec.withText(
                "Die Anmeldefrist ist abgelaufen. Sie können Ihre Auswahl ansehen, aber keine Änderungen vornehmen."));
        assertThat(closedNotifications).isEmpty();

        // Get all checkboxes (event 4 is mandatory, so first checkbox will be disabled)
        var checkboxes = _find(Checkbox.class);
        assertThat(checkboxes).hasSize(2);

        // First checkbox is for mandatory event 4 (CIS 2025) - should be disabled
        assertThat(checkboxes.get(0).isEnabled()).isFalse();
        // Second checkbox is for optional event 5 - should be enabled
        assertThat(checkboxes.get(1).isEnabled()).isTrue();

        // Button text is "Anmeldung aktualisieren" because there are existing
        // registrations
        var button = _get(Button.class, spec -> spec.withText("Anmeldung aktualisieren"));
        assertThat(button.isEnabled()).isTrue();

        // Verify we can interact with the optional checkbox
        _click(checkboxes.get(1));

        // Verify we can click the submit button
        _click(button);
        assertThat(button.getText()).isEqualTo("Die Antwort wurde aktualisiert");
    }

    @Test
    void mandatory_event_is_pre_checked_and_disabled() {
        // Test data: registration 3 includes event 4 (CIS 2025) which is mandatory
        // and event 5 (Jugendmeisterschaft 2025) which is optional
        UI.getCurrent().navigate("public/openregistrationlink123456789");

        // Get all checkboxes
        var checkboxes = _find(Checkbox.class);
        assertThat(checkboxes).hasSize(2); // Two events in this registration

        // First checkbox should be for event 4 (CIS 2025 - mandatory)
        // It should be pre-checked and disabled
        var firstCheckbox = checkboxes.get(0);
        assertThat(firstCheckbox.getValue()).isTrue();
        assertThat(firstCheckbox.isEnabled()).isFalse();

        // Second checkbox should be for event 5 (Jugendmeisterschaft 2025 - optional)
        // It should be enabled and can be toggled
        var secondCheckbox = checkboxes.get(1);
        assertThat(secondCheckbox.isEnabled()).isTrue();
    }

}