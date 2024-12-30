package ch.martinelli.oss.registration.ui.views.registration;

import ch.martinelli.oss.registration.ui.views.KaribuTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H2;
import org.junit.jupiter.api.Test;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

class PublicEventRegistrationViewTest extends KaribuTest {

    @Test
    void page_displayed() {
        UI.getCurrent().navigate(PublicEventRegistrationView.class);


        H2 title = _get(H2.class);
        assertThat(title.getText()).isEqualTo("This place intentionally left empty");
    }
}