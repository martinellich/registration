package ch.martinelli.oss.registration.ui.views.registration;

import ch.martinelli.oss.registration.ui.views.KaribuTest;
import com.github.mvysny.kaributesting.v10.LocatorJ;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.ListItem;
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
    void navigation_with_correct_parameter() {
        UI.getCurrent().navigate("public/550e8400e29b41d4a716446655440000");

        ListItem title = _get(ListItem.class, spec -> spec.withText("Lane Eula"));
        assertThat(title).isNotNull();

        List<Checkbox> checkboxes = LocatorJ._find(Checkbox.class);
        checkboxes.forEach(LocatorJ::_click);

        _click(_get(Button.class, spec -> spec.withText("Anmeldung aktualisieren")));

        Button button = _get(Button.class);
        assertThat(button.getText()).isEqualTo("Die Anmeldung wurde aktualisiert");
    }
}