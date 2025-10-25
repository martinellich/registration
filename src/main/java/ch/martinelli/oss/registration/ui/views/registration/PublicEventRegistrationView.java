package ch.martinelli.oss.registration.ui.views.registration;

import ch.martinelli.oss.registration.db.tables.records.EventRecord;
import ch.martinelli.oss.registration.db.tables.records.EventRegistrationRecord;
import ch.martinelli.oss.registration.db.tables.records.PersonRecord;
import ch.martinelli.oss.registration.db.tables.records.RegistrationEmailRecord;
import ch.martinelli.oss.registration.domain.EventRegistrationRepository;
import ch.martinelli.oss.registration.domain.RegistrationEmailRepository;
import ch.martinelli.oss.registration.domain.RegistrationRepository;
import ch.martinelli.oss.registration.domain.RegistrationService;
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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static ch.martinelli.oss.registration.ui.components.DateFormat.DATE_FORMAT;
import static com.vaadin.flow.i18n.I18NProvider.translate;

@AnonymousAllowed
@Route(value = "public", autoLayout = false)
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
        var optionalRegistrationEmail = registrationEmailRepository.findByLink(parameter);
        if (optionalRegistrationEmail.isPresent()) {
            registrationEmail = optionalRegistrationEmail.get();
            showRegistrationForm();
        }
        else {
            event.rerouteToError(NotFoundException.class);
        }
    }

    @SuppressWarnings("java:S3776")
    private void showRegistrationForm() {
        removeAll();

        var registration = registrationRepository.findById(registrationEmail.getRegistrationId()).orElseThrow();

        // Check if registration period has ended
        var isRegistrationClosed = LocalDate.now().isAfter(registration.getOpenUntil());

        var logo = new Image("/icons/icon.png", "Logo");
        logo.setHeight("40px");
        var header = new HorizontalLayout(logo,
                new H2("%s %d".formatted(registration.getTitle(), registration.getYear())));
        add(header);

        // Show notification banner if registration is closed
        if (isRegistrationClosed) {
            var closedNotification = new Div();
            closedNotification.addClassNames(LumoUtility.Background.ERROR_10, LumoUtility.Padding.MEDIUM,
                    LumoUtility.BorderRadius.MEDIUM, LumoUtility.Margin.Bottom.MEDIUM);
            closedNotification.setText(translate("registration.closed"));
            add(closedNotification);
        }

        if (registration.getRemarks() != null) {
            add(new Paragraph(registration.getRemarks()));
        }

        var persons = registrationEmailRepository.findPersonsByRegistrationEmailId(registrationEmail.getId());

        add(new H3(translate("registration.for")));

        var unorderedList = new UnorderedList();
        unorderedList.addClassName(LumoUtility.Margin.XSMALL);
        add(unorderedList);

        for (var person : persons) {
            var listItem = new ListItem("%s %s".formatted(person.getLastName(), person.getFirstName()));
            unorderedList.add(listItem);
        }

        add(new Hr());
        add(new H2(translate("events")));

        var events = registrationRepository.findAllEventsByRegistrationId(registrationEmail.getRegistrationId());

        for (var event : events) {
            var checkboxes = new FormLayout();
            for (var person : persons) {
                String text;
                if (persons.size() > 1) {
                    text = person.getLastName() + " " + person.getFirstName();
                }
                else {
                    text = translate("participates");
                }
                var checkbox = new Checkbox(text);
                checkbox.getElement().getThemeList().add("switch");
                checkbox.setWidth("200px");
                checkbox.setEnabled(!isRegistrationClosed);
                checkboxes.add(checkbox);
                checkboxMap.put(checkbox, new EventWithPerson(event, person));

                eventRegistrationRepository.findByEventIdAndPersonId(event.getId(), person.getId())
                    .ifPresent(eventRegistration -> checkbox.setValue(eventRegistration.getRegistered()));
            }

            var titleSpan = new Span(event.getTitle());
            titleSpan.addClassName(LumoUtility.FontWeight.BOLD);
            var remarksSpan = new Span(event.getDescription());
            Span dateSpan;
            if (event.getToDate() != null) {
                dateSpan = new Span("%s - %s".formatted(DATE_FORMAT.format(event.getFromDate()),
                        DATE_FORMAT.format(event.getToDate())));
            }
            else {
                dateSpan = new Span(DATE_FORMAT.format(event.getFromDate()));
            }

            var formLayout = new FormLayout();
            formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                    new FormLayout.ResponsiveStep("1024px", 4));
            formLayout.add(titleSpan, dateSpan, remarksSpan, checkboxes);
            add(formLayout);
        }

        add(new Hr());

        var registerButton = new Button();
        if (hasRegistrations()) {
            registerButton.setText(translate("update.registration"));
        }
        else {
            registerButton.setText(translate("send"));
        }
        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerButton.setEnabled(!isRegistrationClosed);
        registerButton.addClickListener(e -> {
            var eventRegistrations = new HashSet<EventRegistrationRecord>();
            for (var entry : checkboxMap.entrySet()) {
                var eventRegistration = new EventRegistrationRecord();
                eventRegistration.setRegistrationId(registration.getId());
                eventRegistration.setEventId(entry.getValue().event().getId());
                eventRegistration.setPersonId(entry.getValue().person().getId());
                eventRegistration.setRegistered(entry.getKey().getValue());
                eventRegistrations.add(eventRegistration);
            }
            registrationService.register(registrationEmail.getId(), eventRegistrations);

            if (hasRegistrations()) {
                registerButton.setText(translate("registration.updated"));
            }
            else {
                registerButton.setText(translate("registration.success"));
            }

            registerButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        });
        add(registerButton);
    }

    private boolean hasRegistrations() {
        return checkboxMap.keySet().stream().anyMatch(Checkbox::getValue);
    }

    @Override
    public String getPageTitle() {
        return "registration";
    }

    public record EventWithPerson(EventRecord event, PersonRecord person) {
    }

}
