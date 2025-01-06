package ch.martinelli.oss.registration.ui.views.registration;

import ch.martinelli.oss.registration.db.tables.records.*;
import ch.martinelli.oss.registration.domain.EventRegistrationRepository;
import ch.martinelli.oss.registration.domain.RegistrationEmailRepository;
import ch.martinelli.oss.registration.domain.RegistrationRepository;
import ch.martinelli.oss.registration.domain.RegistrationService;
import ch.martinelli.oss.registration.ui.components.Notification;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.*;

import static ch.martinelli.oss.registration.ui.components.DateFormat.DATE_FORMAT;
import static com.vaadin.flow.i18n.I18NProvider.translate;

@Route(value = "public", autoLayout = false)
@AnonymousAllowed
public class PublicEventRegistrationView extends VerticalLayout implements HasUrlParameter<String>, HasDynamicTitle {

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
        HorizontalLayout header = new HorizontalLayout(logo, new H1("%s %d".formatted(registration.getTitle(), registration.getYear())));
        add(header);
        if (registration.getRemarks() != null) {
            add(new Paragraph(registration.getRemarks()));
        }

        List<PersonRecord> persons = registrationEmailRepository.findPersonsByRegistrationEmailId(registrationEmail.getId());

        add(new H3(translate("registration.for")));
        UnorderedList unorderedList = new UnorderedList();
        add(unorderedList);
        for (PersonRecord person : persons) {
            ListItem listItem = new ListItem("%s %s".formatted(person.getLastName(), person.getFirstName()));
            unorderedList.add(listItem);
        }

        add(new Hr());
        add(new H2(translate("events")));

        List<EventRecord> events = registrationRepository.findAllEventsByRegistrationId(registrationEmail.getRegistrationId());

        for (EventRecord event : events) {
            HorizontalLayout checkboxes = new HorizontalLayout();
            for (PersonRecord person : persons) {
                String text;
                if (persons.size() > 1) {
                    text = person.getLastName() + " " + person.getFirstName();
                } else {
                    text = translate("participates");
                }
                Checkbox checkbox = new Checkbox(text);
                checkbox.getElement().getThemeList().add("switch");
                checkbox.setWidth("200px");
                checkboxes.add(checkbox);
                checkboxMap.put(checkbox, new EventWithPerson(event, person));

                eventRegistrationRepository.findByEventIdAndPersonId(event.getId(), person.getId())
                        .ifPresent(eventRegistration -> checkbox.setValue(eventRegistration.getRegistered()));
            }

            Span titleSpan = new Span(event.getTitle());
            titleSpan.addClassName(LumoUtility.FontWeight.BOLD);
            titleSpan.setWidth("300px");
            Span dateSpan = new Span(DATE_FORMAT.format(event.getFromDate()));
            dateSpan.setWidth("150px");

            FormLayout formLayout = new FormLayout();
            formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("600px", 3));
            formLayout.add(titleSpan, dateSpan, checkboxes);
            add(formLayout);
        }

        add(new Hr());

        Button registerButton = new Button(translate("register"));
        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerButton.addClickListener(e -> {
            Set<EventRegistrationRecord> eventRegistrations = new HashSet<>();
            for (Map.Entry<Checkbox, EventWithPerson> entry : checkboxMap.entrySet()) {
                EventRegistrationRecord eventRegistration = new EventRegistrationRecord();
                eventRegistration.setRegistrationId(registration.getId());
                eventRegistration.setEventId(entry.getValue().event().getId());
                eventRegistration.setPersonId(entry.getValue().person().getId());
                eventRegistration.setRegistered(entry.getKey().getValue());
                eventRegistrations.add(eventRegistration);
            }
            registrationService.register(eventRegistrations);
            Notification.success(translate("registration.success"));
            registerButton.setText(translate("registration.success"));
            registerButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            registerButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        });
        add(registerButton);
    }

    @Override
    public String getPageTitle() {
        return "registration";
    }

    public record EventWithPerson(EventRecord event, PersonRecord person) {
    }
}
