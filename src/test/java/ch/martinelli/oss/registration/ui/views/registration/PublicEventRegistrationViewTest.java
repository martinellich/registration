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
        checkboxes.forEach(checkbox -> LocatorJ._click(checkbox));

        _click(_get(Button.class, spec -> spec.withText("Absenden")));

        var button = _get(Button.class);
        assertThat(button.getText()).isEqualTo("Vielen Dank! Ihre Antwort wurde gesendet.");
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

        // Verify all checkboxes are enabled
        var checkboxes = _find(Checkbox.class);
        assertThat(checkboxes).allMatch(Checkbox::isEnabled);

        // Verify the submit button is enabled
        var button = _get(Button.class, spec -> spec.withText("Absenden"));
        assertThat(button.isEnabled()).isTrue();

        // Verify we can interact with checkboxes
        checkboxes.forEach(checkbox -> _click(checkbox));

        // Verify we can click the submit button
        _click(button);
        assertThat(button.getText()).isEqualTo("Vielen Dank! Ihre Antwort wurde gesendet.");
    }

}