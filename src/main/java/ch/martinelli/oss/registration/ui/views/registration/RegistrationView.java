package ch.martinelli.oss.registration.ui.views.registration;

import ch.martinelli.oss.registration.db.tables.records.EventRecord;
import ch.martinelli.oss.registration.db.tables.records.PersonRecord;
import ch.martinelli.oss.registration.db.tables.records.RegistrationRecord;
import ch.martinelli.oss.registration.db.tables.records.RegistrationViewRecord;
import ch.martinelli.oss.registration.domain.EventRepository;
import ch.martinelli.oss.registration.domain.PersonRepository;
import ch.martinelli.oss.registration.domain.RegistrationRepository;
import ch.martinelli.oss.registration.domain.RegistrationService;
import ch.martinelli.oss.registration.ui.components.Notification;
import ch.martinelli.oss.vaadinjooq.util.VaadinJooqUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.RolesAllowed;
import org.jooq.impl.DSL;
import org.springframework.dao.DataIntegrityViolationException;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static ch.martinelli.oss.registration.db.tables.Event.EVENT;
import static ch.martinelli.oss.registration.db.tables.Person.PERSON;
import static ch.martinelli.oss.registration.db.tables.Registration.REGISTRATION;
import static ch.martinelli.oss.registration.db.tables.RegistrationView.REGISTRATION_VIEW;

@PageTitle("Tätigkeitsprogramm")
@Route("registrations/:registrationID?/:action?(edit)")
@Menu(order = 2, icon = LineAwesomeIconUrl.LIST_SOLID)
@RolesAllowed("ADMIN")
public class RegistrationView extends Div implements BeforeEnterObserver {

    private static final String REGISTRATION_ID = "registrationID";
    private static final String REGISTRATION_EDIT_ROUTE_TEMPLATE = "registrations/%s/edit";

    private final Grid<RegistrationViewRecord> grid = new Grid<>(RegistrationViewRecord.class, false);
    private final Binder<RegistrationRecord> binder = new Binder<>(RegistrationRecord.class);

    private final Button saveButton = new Button("Speichern");
    private final Button cancelButton = new Button("Abbrechen");
    private final Button createMailingButton = new Button("Versand erstellen");
    private final Button sendEmailsButton = new Button("Emails verschicken");

    private RegistrationRecord registration;

    private final transient RegistrationService registrationService;
    private final transient RegistrationRepository registrationRepository;
    private final transient EventRepository eventRepository;
    private final transient PersonRepository personRepository;

    private MultiSelectListBox<EventRecord> eventListBox;
    private MultiSelectListBox<PersonRecord> personListBox;

    public RegistrationView(RegistrationService registrationService, RegistrationRepository registrationRepository,
                            EventRepository eventRepository, PersonRepository personRepository) {
        this.registrationService = registrationService;
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.personRepository = personRepository;

        addClassNames("registrations-view");

        // Create UI
        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setOrientation(SplitLayout.Orientation.VERTICAL);
        splitLayout.setSplitterPosition(20);

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        add(splitLayout);

        configureGrid();
        configureButtons(registrationService);
    }

    private void configureGrid() {
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        grid.addColumn(RegistrationViewRecord::getYear)
                .setSortable(true).setSortProperty(REGISTRATION.YEAR.getName())
                .setHeader("Jahr").setAutoWidth(true);
        grid.addColumn(RegistrationViewRecord::getOpenFrom)
                .setSortable(true).setSortProperty(REGISTRATION.OPEN_FROM.getName())
                .setHeader("Offen von").setAutoWidth(true);
        grid.addColumn(RegistrationViewRecord::getOpenUntil)
                .setSortable(true).setSortProperty(REGISTRATION.OPEN_UNTIL.getName())
                .setHeader("Offen bis").setAutoWidth(true);
        grid.addComponentColumn(r -> createIcon(r.getEmailCreatedCount()))
                .setHeader("Versand erstellt").setAutoWidth(true);
        grid.addComponentColumn(r -> createIcon(r.getEmailSentCount()))
                .setHeader("Emails verschickt").setAutoWidth(true);

        loadData();

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            RegistrationViewRecord registrationViewRecord = event.getValue();

            if (registrationViewRecord != null) {
                UI.getCurrent().navigate(String.format(REGISTRATION_EDIT_ROUTE_TEMPLATE, registrationViewRecord.getId()));
            } else {
                clearForm();
                UI.getCurrent().navigate(RegistrationView.class);
            }
        });
    }

    private Icon createIcon(long value) {
        return value > 0
                ? applyTheme(VaadinIcon.CHECK_SQUARE.create(), "badge success")
                : applyTheme(VaadinIcon.THIN_SQUARE.create(), "badge contrast");
    }

    private Icon applyTheme(Icon icon, String theme) {
        icon.getElement().getThemeList().add(theme);
        return icon;
    }

    private void loadData() {
        grid.setItems(query -> registrationRepository.findAllFromView(
                        DSL.noCondition(),
                        query.getOffset(), query.getLimit(),
                        VaadinJooqUtil.orderFields(REGISTRATION_VIEW, query))
                .stream());
    }

    private void setButtonState(RegistrationViewRecord registrationViewRecord) {
        if (binder.hasChanges()) {
            createMailingButton.setEnabled(false);
            sendEmailsButton.setEnabled(false);
        } else {
            boolean eventListBoxEmpty = eventListBox.getSelectedItems().isEmpty();
            boolean personListBoxEmpty = personListBox.getSelectedItems().isEmpty();
            boolean isEmailCreatedCountZero = registrationViewRecord != null && registrationViewRecord.getEmailCreatedCount() == 0;
            createMailingButton.setEnabled(!eventListBoxEmpty && !personListBoxEmpty && isEmailCreatedCountZero);
            sendEmailsButton.setEnabled(registrationViewRecord != null && registrationViewRecord.getEmailCreatedCount() > 0 && registrationViewRecord.getEmailSentCount() == 0);
        }
    }


    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> eventId = event.getRouteParameters().get(REGISTRATION_ID).map(Long::parseLong);
        if (eventId.isPresent()) {
            Optional<RegistrationRecord> registrationFromBackend = registrationRepository.findById(eventId.get());
            if (registrationFromBackend.isPresent()) {
                populateForm(registrationFromBackend.get());
                registrationRepository.findByIdFromView(eventId.get()).ifPresent(registrationViewRecord -> {
                    grid.select(registrationViewRecord);
                    setButtonState(registrationViewRecord);
                });
            } else {
                Notification.error(String.format("Die angeforderte Registrierung wurde nicht gefunden, ID = %s", eventId.get()));
                // when a row is selected but the data is no longer available, refresh grid
                refreshGrid();
                event.forwardTo(RegistrationView.class);
            }
        } else {
            setButtonState(null);
            clearForm();
        }
    }

    private void configureButtons(RegistrationService registrationService) {
        saveButton.addClickListener(e -> {
            try {
                if (this.registration == null) {
                    this.registration = new RegistrationRecord();
                }
                if (binder.validate().isOk()) {
                    binder.writeBean(this.registration);
                    registrationService.save(this.registration,
                            this.eventListBox.getSelectedItems(), this.personListBox.getSelectedItems());

                    clearForm();
                    refreshGrid();

                    Notification.success("Die Daten wurden gespeichert");
                    UI.getCurrent().navigate(RegistrationView.class);
                }
            } catch (DataIntegrityViolationException | ValidationException dataIntegrityViolationException) {
                Notification.error("Fehler beim Aktualisieren der Daten. Überprüfen Sie, ob alle Werte gültig sind");
            }
        });

        cancelButton.addClickListener(e -> {
            clearForm();
            refreshGrid();
        });

        createMailingButton.addClickListener(e -> {
            if (this.registration != null) {
                new ConfirmDialog("Versand erstellen",
                        "Möchtest du den Versand für die Registrierung erstellen?",
                        "Ja",
                        confirmEvent -> {
                            if (registrationService.createMailing(this.registration)) {
                                Notification.success("Der Versand wurde erstellt");
                                refreshGrid();
                            } else {
                                Notification.error("Es gibt bereits einen Versand");
                            }
                        },
                        "Abbrechen",
                        cancelEvent -> {
                        }).open();
            }
        });

        sendEmailsButton.addClickListener(e -> {
            if (this.registration != null) {
                new ConfirmDialog("Emails versenden",
                        "Möchtest du die Emails verschicken?",
                        "Ja",
                        confirmEvent -> {
                            if (registrationService.sendMails(this.registration)) {
                                Notification.success("Die Emails werden versendet");
                            } else {
                                Notification.error("Die Emails wurden bereits versendet");
                            }
                        },
                        "Abbrechen",
                        cancelEvent -> {
                        }).open();
            }
        });
    }

    private void createEditorLayout(SplitLayout splitLayout) {
        Div editorLayoutDiv = new Div();
        editorLayoutDiv.setClassName("editor-layout");

        Div editorDiv = new Div();
        editorDiv.setClassName("editor");
        editorLayoutDiv.add(editorDiv);

        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 3));

        IntegerField yearIntegerField = new IntegerField("Jahr");
        binder.forField(yearIntegerField)
                .asRequired()
                .bind(RegistrationRecord::getYear, RegistrationRecord::setYear);
        yearIntegerField.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                loadEvents(yearIntegerField.getValue());
                eventListBox.clear();
            }
        });

        DatePicker openFromDatePicker = new DatePicker("Offen von");
        binder.forField(openFromDatePicker)
                .asRequired()
                .bind(RegistrationRecord::getOpenFrom, RegistrationRecord::setOpenFrom);

        DatePicker openUntilDatePicker = new DatePicker("Offen bis");
        binder.forField(openUntilDatePicker)
                .asRequired()
                .bind(RegistrationRecord::getOpenUntil, RegistrationRecord::setOpenUntil);

        formLayout.add(yearIntegerField, openFromDatePicker, openUntilDatePicker);
        editorDiv.add(formLayout);

        editorDiv.add(new Hr());

        FormLayout listBoxFormLayout = new FormLayout();
        listBoxFormLayout.add(new H3("Anlässe"), new H3("Jugeler"));

        eventListBox = new MultiSelectListBox<>();
        eventListBox.setId("event-list-box");
        eventListBox.setItemLabelGenerator(EventRecord::getTitle);
        eventListBox.addValueChangeListener(e -> setButtonState(grid.asSingleSelect().getValue()));

        Scroller eventListBoxScroller = new Scroller(eventListBox);
        eventListBoxScroller.addClassName("scroller");
        eventListBoxScroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);

        personListBox = new MultiSelectListBox<>();
        personListBox.setId("person-list-box");
        personListBox.setItemLabelGenerator(p -> "%s %s".formatted(p.getLastName(), p.getFirstName()));
        personListBox.addValueChangeListener(e -> setButtonState(grid.asSingleSelect().getValue()));

        Scroller personListBoxScroller = new Scroller(personListBox);
        personListBoxScroller.addClassName("scroller");
        personListBoxScroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);

        listBoxFormLayout.add(eventListBoxScroller, personListBoxScroller);

        editorDiv.add(listBoxFormLayout);

        createButtonLayout(editorLayoutDiv);

        splitLayout.addToSecondary(editorLayoutDiv);
    }

    private void createButtonLayout(Div editorLayoutDiv) {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        createMailingButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        createMailingButton.setEnabled(false);
        sendEmailsButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        sendEmailsButton.setEnabled(false);
        buttonLayout.add(saveButton, cancelButton, createMailingButton, sendEmailsButton);
        editorLayoutDiv.add(buttonLayout);
    }

    private void createGridLayout(SplitLayout splitLayout) {
        Div wrapper = new Div();
        wrapper.setClassName("grid-wrapper");
        splitLayout.addToPrimary(wrapper);
        wrapper.add(grid);
    }

    private void refreshGrid() {
        loadData();
    }

    private void clearForm() {
        populateForm(null);
    }

    private void populateForm(RegistrationRecord value) {
        this.registration = value;
        binder.readBean(this.registration);

        personListBox.setItems(personRepository.findAll(PERSON.ACTIVE.isTrue(), List.of(PERSON.LAST_NAME, PERSON.FIRST_NAME)));

        if (value == null) {
            eventListBox.clear();
            personListBox.clear();
        } else {
            loadEvents(this.registration.getYear());
            personListBox.setValue(registrationService.findPersonsByRegistrationId(this.registration.getId()));
            eventListBox.setValue(registrationService.findEventsByRegistrationId(this.registration.getId()));
        }
    }

    private void loadEvents(Integer year) {
        LocalDate fromDate = LocalDate.of(year, 1, 1);
        LocalDate toDate = LocalDate.of(year, 12, 31);

        eventListBox.setItems(eventRepository.findAll(EVENT.FROM_DATE.between(fromDate, toDate), List.of(EVENT.TITLE)));
    }

}
