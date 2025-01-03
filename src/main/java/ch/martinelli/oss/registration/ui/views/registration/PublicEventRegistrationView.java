package ch.martinelli.oss.registration.ui.views.registration;

import ch.martinelli.oss.registration.db.tables.records.EventRecord;
import ch.martinelli.oss.registration.db.tables.records.PersonRecord;
import ch.martinelli.oss.registration.db.tables.records.RegistrationEmailRecord;
import ch.martinelli.oss.registration.db.tables.records.RegistrationRecord;
import ch.martinelli.oss.registration.domain.EventRegistrationRepository;
import ch.martinelli.oss.registration.domain.RegistrationEmailRepository;
import ch.martinelli.oss.registration.domain.RegistrationRepository;
import ch.martinelli.oss.registration.domain.RegistrationService;
import ch.martinelli.oss.registration.ui.components.Notification;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@PageTitle("Anmeldung")
@Route(value = "public", autoLayout = false)
@AnonymousAllowed
public class PublicEventRegistrationView extends VerticalLayout implements HasUrlParameter<String> {

    private final transient RegistrationEmailRepository registrationEmailRepository;
    private final transient RegistrationRepository registrationRepository;
    private final transient RegistrationService registrationService;
    private final transient EventRegistrationRepository eventRegistrationRepository;

    private final transient Map<Checkbox, EventWithPerson> checkboxMap = new HashMap<>();

    private RegistrationEmailRecord registrationEmail;

    public PublicEventRegistrationView(RegistrationEmailRepository registrationEmailRepository,
                                       RegistrationRepository registrationRepository, RegistrationService registrationService,
                                       EventRegistrationRepository eventRegistrationRepository) {
        this.registrationEmailRepository = registrationEmailRepository;
        this.registrationRepository = registrationRepository;
        this.registrationService = registrationService;
        this.eventRegistrationRepository = eventRegistrationRepository;
    }

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        Optional<RegistrationEmailRecord> optionalRegistrationEmail = registrationEmailRepository.findByLink(parameter);
        if (optionalRegistrationEmail.isPresent()) {
            registrationEmail = optionalRegistrationEmail.get();
            showRegistrationForm();
        } else {
            event.rerouteToError(NotFoundException.class);
        }
    }

    private void showRegistrationForm() {
        removeAll();

        RegistrationRecord registration = registrationRepository.findById(registrationEmail.getRegistrationId()).orElseThrow();

        Image logo = new Image("/icons/icon.png", "Logo");
        logo.setHeight("50px");
        HorizontalLayout header = new HorizontalLayout(logo, new H1("Jugi TV Erlach - Anmeldung %d".formatted(registration.getYear())));
        add(header);
        if (registration.getRemarks() != null) {
            add(new Paragraph(registration.getRemarks()));
        }

        List<PersonRecord> persons = registrationEmailRepository.findPersonsByRegistrationEmailId(registrationEmail.getId());

        add(new H3("Anmeldung für"));
        UnorderedList unorderedList = new UnorderedList();
        add(unorderedList);
        for (PersonRecord person : persons) {
            ListItem listItem = new ListItem("%s %s".formatted(person.getLastName(), person.getFirstName()));
            unorderedList.add(listItem);
        }

        add(new Hr());
        add(new H2("Anlässe"));

        List<EventRecord> events = registrationRepository.findAllEventsByRegistrationId(registrationEmail.getRegistrationId());

        for (EventRecord event : events) {
            HorizontalLayout checkboxes = new HorizontalLayout();
            for (PersonRecord person : persons) {
                String text;
                if (persons.size() > 1) {
                    text = person.getLastName() + " " + person.getFirstName();
                } else {
                    text = "nimmt teil";
                }
                Checkbox checkbox = new Checkbox(text);
                checkbox.setWidth("200px");
                checkboxes.add(checkbox);
                checkboxMap.put(checkbox, new EventWithPerson(event, person));

                eventRegistrationRepository.findByEventIdAndPersonId(event.getId(), person.getId())
                        .ifPresent(eventRegistration -> checkbox.setValue(eventRegistration.getRegistered()));
            }
            Span titleSpan = new Span(event.getTitle());
            titleSpan.setWidth("300px");
            Span dateSpan = new Span(event.getFromDate().toString());
            dateSpan.setWidth("150px");
            HorizontalLayout line = new HorizontalLayout(titleSpan, dateSpan, checkboxes);
            add(line);
        }

        add(new Hr());

        Button registerButton = new Button("Anmelden");
        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerButton.addClickListener(e -> {
            for (Map.Entry<Checkbox, EventWithPerson> entry : checkboxMap.entrySet()) {
                registrationService.register(registrationEmail.getRegistrationId(), entry.getValue().event(), entry.getValue().person(), entry.getKey().getValue());
            }
            Notification.success("Vielen Dank für die Anmeldung!");
        });
        add(registerButton);
    }
}
