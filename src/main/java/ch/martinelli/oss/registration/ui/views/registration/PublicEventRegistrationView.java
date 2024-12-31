package ch.martinelli.oss.registration.ui.views.registration;

import ch.martinelli.oss.registration.db.tables.records.RegistrationEmailRecord;
import ch.martinelli.oss.registration.domain.RegistrationEmailRepository;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.util.Optional;

@PageTitle("Anmeldung")
@Route(value = "public", autoLayout = false)
@AnonymousAllowed
public class PublicEventRegistrationView extends VerticalLayout implements HasUrlParameter<String> {

    private final RegistrationEmailRepository registrationEmailRepository;

    public PublicEventRegistrationView(RegistrationEmailRepository registrationEmailRepository) {
        this.registrationEmailRepository = registrationEmailRepository;

        add(new H2("Anmeldung"));
    }

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        Optional<RegistrationEmailRecord> optionalRegistrationEmail = registrationEmailRepository.findByLink(parameter);
        if (optionalRegistrationEmail.isPresent()) {
            RegistrationEmailRecord registrationEmail = optionalRegistrationEmail.get();
        } else {
            event.rerouteToError(NotFoundException.class);
        }
    }
}
