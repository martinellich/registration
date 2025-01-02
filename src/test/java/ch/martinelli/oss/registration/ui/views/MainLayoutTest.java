package ch.martinelli.oss.registration.ui.views;

import ch.martinelli.oss.registration.ui.views.registration.RegistrationView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.MenuItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;

class MainLayoutTest extends KaribuTest{

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
    }
}