package ch.martinelli.oss.registration.ui.views.registration;

import ch.martinelli.oss.registration.ui.views.KaribuTest;
import com.github.mvysny.kaributesting.v10.LocatorJ;
import com.github.mvysny.kaributesting.v10.NotificationsKt;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H2;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class PublicEventRegistrationViewTest extends KaribuTest {

    @Test
    void navigation_without_parameter() {
        UI ui = UI.getCurrent();
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ui.navigate(PublicEventRegistrationView.class))
                .withMessage("Navigation target 'ch.martinelli.oss.registration.ui.views.registration.PublicEventRegistrationView' requires a parameter.");
    }

    @Test
    void navgation_with_correct_parameter() {
        UI.getCurrent().navigate("public/550e8400e29b41d4a716446655440000");

        H2 title = _get(H2.class, spec -> spec.withText("Lane Eula"));
        assertThat(title).isNotNull();

        List<Checkbox> checkboxes = LocatorJ._find(Checkbox.class);
        checkboxes.forEach(LocatorJ::_click);

        _click(_get(Button.class, spec -> spec.withText("Anmelden")));

        NotificationsKt.expectNotifications("Vielen Dank für die Anmeldung!");
    }
}