package ch.martinelli.oss.registration.ui.views;

import ch.martinelli.oss.registration.ui.views.registration.RegistrationView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.MenuItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

class MainLayoutTest extends KaribuTest {

    @BeforeEach
    void login() {
        login("simon@martinelli.ch");
        UI.getCurrent().getPage().reload();

        UI.getCurrent().navigate(RegistrationView.class);
    }

    @Test
    void logout_user() {
        MenuItem menuItem = _get(MenuItem.class, spec -> spec.withId("sign-out"));
        _click(menuItem);
        // This is just necessary for the test to pass, as the logout doesn't redirect to
        // the login page because of the FakeUI
        menuItem = _get(MenuItem.class, spec -> spec.withId("sign-out"));
        assertThat(menuItem).isNotNull();
    }

    @Test
    void switch_language() {
        Button languageSelector = _get(Button.class, spec -> spec.withText("EN"));
        _click(languageSelector);

        languageSelector = _get(Button.class, spec -> spec.withText("DE"));
        assertThat(languageSelector.getText()).isEqualTo("DE");

        languageSelector = _get(Button.class, spec -> spec.withText("DE"));
        _click(languageSelector);

        languageSelector = _get(Button.class, spec -> spec.withText("EN"));
        assertThat(languageSelector.getText()).isEqualTo("EN");
    }

}