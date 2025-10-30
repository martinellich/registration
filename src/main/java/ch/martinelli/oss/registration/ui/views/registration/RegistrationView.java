package ch.martinelli.oss.registration.ui.views.registration;

import ch.martinelli.oss.registration.db.tables.records.EventRecord;
import ch.martinelli.oss.registration.db.tables.records.PersonRecord;
import ch.martinelli.oss.registration.db.tables.records.RegistrationRecord;
import ch.martinelli.oss.registration.db.tables.records.RegistrationViewRecord;
import ch.martinelli.oss.registration.domain.EventRepository;
import ch.martinelli.oss.registration.domain.PersonRepository;
import ch.martinelli.oss.registration.domain.RegistrationRepository;
import ch.martinelli.oss.registration.domain.RegistrationService;
import ch.martinelli.oss.registration.security.Roles;
import ch.martinelli.oss.registration.security.SecurityContext;
import ch.martinelli.oss.registration.ui.components.I18nDatePicker;
import ch.martinelli.oss.registration.ui.components.Icon;
import ch.martinelli.oss.registration.ui.components.Notification;
import ch.martinelli.oss.vaadinjooq.util.VaadinJooqUtil;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.router.*;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.jooq.impl.DSL;
import org.springframework.dao.DataIntegrityViolationException;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

import static ch.martinelli.oss.registration.db.tables.Event.EVENT;
import static ch.martinelli.oss.registration.db.tables.Person.PERSON;
import static ch.martinelli.oss.registration.db.tables.Registration.REGISTRATION;
import static ch.martinelli.oss.registration.db.tables.RegistrationView.REGISTRATION_VIEW;
import static ch.martinelli.oss.registration.ui.components.DateFormat.DATE_FORMAT;
import static ch.martinelli.oss.registration.ui.views.EditView.ACTION_ICON;
import static ch.martinelli.oss.registration.ui.views.registration.RegistrationView.ID;
import static com.vaadin.flow.i18n.I18NProvider.translate;

@RolesAllowed({ Roles.USER, Roles.ADMIN })
@RouteAlias("")
@Route("registrations/:" + ID + "?")
public class RegistrationView extends Div implements BeforeEnterObserver, HasDynamicTitle {

    public static final String ID = "id";

    private static final String CANCEL = "cancel";

    private static final String CREATE_MAILING = "create.mailing";

    private final transient RegistrationService registrationService;

    private final transient RegistrationRepository registrationRepository;

    private final transient EventRepository eventRepository;

    private final transient PersonRepository personRepository;

    private final transient SecurityContext securityContext;

    private final Grid<RegistrationViewRecord> grid = new Grid<>(RegistrationViewRecord.class, false);

    private MultiSelectListBox<EventRecord> eventListBox;

    private MultiSelectListBox<PersonRecord> personListBox;

    private final Button saveButton = new Button(translate("save"));

    private final Button cancelButton = new Button(translate(CANCEL));

    private final Button createMailingButton = new Button(translate(CREATE_MAILING));

    private final Button sendEmailsButton = new Button(translate("send.emails"));

    private FormLayout formLayout;

    private final Binder<RegistrationRecord> binder = new Binder<>(RegistrationRecord.class);

    private RegistrationRecord registration;

    private boolean dirty;

    private boolean hidePastInvitations = true;

    public RegistrationView(RegistrationService registrationService, RegistrationRepository registrationRepository,
            EventRepository eventRepository, PersonRepository personRepository, SecurityContext securityContext) {
        this.registrationService = registrationService;
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.personRepository = personRepository;
        this.securityContext = securityContext;

        addClassNames("registrations-view");

        var splitLayout = new SplitLayout();
        splitLayout.setOrientation(SplitLayout.Orientation.VERTICAL);
        splitLayout.setSplitterPosition(20);

        splitLayout.addToPrimary(createGridLayout());
        splitLayout.addToSecondary(createEditorLayout());

        add(splitLayout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        var eventId = event.getRouteParameters().get(ID).map(Long::parseLong);
        if (eventId.isPresent()) {
            var registrationFromBackend = registrationRepository.findById(eventId.get());
            if (registrationFromBackend.isPresent()) {
                populateForm(registrationFromBackend.get());
                registrationRepository.findByIdFromView(eventId.get()).ifPresent(registrationViewRecord -> {
                    grid.select(registrationViewRecord);
                    setButtonState(registrationViewRecord);
                });
            }
            else {
                // when a row is selected but the data is no longer available, refresh
                // grid
                loadData();
                event.forwardTo(RegistrationView.class);
            }
        }
        else {
            setButtonState(null);
            clearForm();
        }
    }

    private Div createGridLayout() {
        var wrapper = new Div();
        wrapper.setClassName("grid-wrapper");

        // Add toolbar with filter button
        var toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);
        toolbar.setPadding(true);
        toolbar.setSpacing(true);

        // When hidePastInvitations is true (default), button should show "Show past
        // invitations" action
        var togglePastInvitationsButton = new Button(translate("show.past.invitations"));
        togglePastInvitationsButton.setId("toggle-past-invitations-button");
        togglePastInvitationsButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        togglePastInvitationsButton.addClickListener(event -> {
            hidePastInvitations = !hidePastInvitations;
            togglePastInvitationsButton
                .setText(hidePastInvitations ? translate("show.past.invitations") : translate("hide.past.invitations"));
            grid.getDataProvider().refreshAll();
        });

        toolbar.add(togglePastInvitationsButton);

        wrapper.add(toolbar, grid);

        configureGrid();

        return wrapper;
    }

    private void configureGrid() {
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        var titleColumn = grid.addColumn(RegistrationViewRecord::getTitle)
            .setSortable(true)
            .setSortProperty(REGISTRATION.TITLE.getName())
            .setHeader(translate("title"));
        var yearColumn = grid.addColumn(RegistrationViewRecord::getYear)
            .setSortable(true)
            .setSortProperty(REGISTRATION.YEAR.getName())
            .setHeader(translate("year"));
        grid.addColumn(registrationViewRecord -> DATE_FORMAT.format(registrationViewRecord.getOpenFrom()))
            .setSortable(true)
            .setSortProperty(REGISTRATION.OPEN_FROM.getName())
            .setHeader(translate("open.from"));
        grid.addColumn(registrationViewRecord -> DATE_FORMAT.format(registrationViewRecord.getOpenUntil()))
            .setSortable(true)
            .setSortProperty(REGISTRATION.OPEN_UNTIL.getName())
            .setHeader(translate("open.until"));
        grid.addComponentColumn(r -> createIcon(r.getEmailCreatedCount())).setHeader(translate("mailing.created"));
        grid.addComponentColumn(r -> createIcon(r.getEmailSentCount())).setHeader(translate("emails.sent"));

        var addIcon = new Icon(LineAwesomeIcon.PLUS_CIRCLE_SOLID, e -> {
            loadData();
            var registrationRecord = new RegistrationRecord();
            populateForm(registrationRecord);
        });
        addIcon.setId("add-icon");
        addIcon.setClassName(ACTION_ICON);

        grid.addComponentColumn(registrationViewRecord -> {
            var buttonLayout = new HorizontalLayout();
            buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

            var showRegistrationsIcon = new Icon(LineAwesomeIcon.TH_LIST_SOLID,
                    e -> UI.getCurrent().navigate(EventRegistrationView.class, registrationViewRecord.getId()));
            showRegistrationsIcon.addClassNames(ACTION_ICON);
            showRegistrationsIcon.setTooltipText(translate("show.registrations"));

            var showMailingsIcon = new Icon(LineAwesomeIcon.MAIL_BULK_SOLID,
                    e -> UI.getCurrent().navigate(RegistrationEmailView.class, registrationViewRecord.getId()));
            showMailingsIcon.addClassNames(ACTION_ICON);
            showMailingsIcon.setTooltipText(translate("show.mailings"));

            var deleteIcon = new Icon(LineAwesomeIcon.TRASH_SOLID, e -> new ConfirmDialog(translate("delete.record"),
                    translate("delete.record.question"), translate("yes"), ce -> {
                        registrationRepository.deleteById(registrationViewRecord.getId());

                        clearForm();
                        loadData();

                        Notification.success(translate("delete.record.success"));
                    }, translate(CANCEL), ce -> {
                    })
                .open());
            deleteIcon.setId("delete-action");
            deleteIcon.addClassName("delete-icon");

            buttonLayout.add(showMailingsIcon, showRegistrationsIcon, deleteIcon);

            return buttonLayout;
        })
            .setHeader(addIcon)
            .setTextAlign(ColumnTextAlign.END)
            .setWidth("140px")
            .setFlexGrow(0)
            .setKey("action-column");

        loadData();

        grid.sort(List.of(new GridSortOrder<>(yearColumn, SortDirection.DESCENDING),
                new GridSortOrder<>(titleColumn, SortDirection.ASCENDING)));

        grid.asSingleSelect().addValueChangeListener(event -> {
            var registrationViewRecord = event.getValue();

            if (registrationViewRecord != null) {
                UI.getCurrent().navigate(RegistrationView.class, new RouteParam(ID, registrationViewRecord.getId()));
            }
            else {
                clearForm();
                UI.getCurrent().navigate(RegistrationView.class);
            }
        });
    }

    private Component createIcon(long value) {
        return value > 0 ? LineAwesomeIcon.CHECK_SOLID.create() : new Span();
    }

    private void loadData() {
        var dataProvider = new CallbackDataProvider<RegistrationViewRecord, Void>(query -> {
            var condition = hidePastInvitations ? REGISTRATION_VIEW.YEAR.greaterOrEqual(LocalDate.now().getYear())
                .and(REGISTRATION_VIEW.OPEN_UNTIL.greaterOrEqual(LocalDate.now())) : DSL.noCondition();
            return registrationRepository
                .findAllFromView(condition, query.getOffset(), query.getLimit(),
                        VaadinJooqUtil.orderFields(REGISTRATION_VIEW, query))
                .stream();
        }, query -> {
            var condition = hidePastInvitations ? REGISTRATION_VIEW.YEAR.greaterOrEqual(LocalDate.now().getYear())
                .and(REGISTRATION_VIEW.OPEN_UNTIL.greaterOrEqual(LocalDate.now())) : DSL.noCondition();
            return registrationRepository.countFromView(condition);
        }, RegistrationViewRecord::getId);
        grid.setDataProvider(dataProvider);
    }

    private Div createEditorLayout() {
        var editorLayoutDiv = new Div();
        editorLayoutDiv.setClassName("editor-layout");

        var editorDiv = new Div();
        editorDiv.setClassName("editor");

        var editorScroller = new Scroller(editorDiv);
        editorScroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        editorScroller.setHeight("calc(100% - 60px)");
        editorLayoutDiv.add(editorScroller);

        formLayout = new FormLayout();
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 4));

        var yearIntegerField = new IntegerField(translate("year"));
        binder.forField(yearIntegerField).asRequired().bind(RegistrationRecord::getYear, RegistrationRecord::setYear);
        yearIntegerField.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                loadEvents(yearIntegerField.getValue());
                eventListBox.clear();
            }
        });

        var openFromDatePicker = new I18nDatePicker(translate("open.from"));
        binder.forField(openFromDatePicker)
            .asRequired()
            .bind(RegistrationRecord::getOpenFrom, RegistrationRecord::setOpenFrom);

        var openUntilDatePicker = new I18nDatePicker(translate("open.until"));
        binder.forField(openUntilDatePicker)
            .asRequired()
            .bind(RegistrationRecord::getOpenUntil, RegistrationRecord::setOpenUntil);

        var titleTextField = new TextField(translate("title"));
        binder.forField(titleTextField).asRequired().bind(RegistrationRecord::getTitle, RegistrationRecord::setTitle);

        var remarks = new TextArea(translate("remarks"));
        remarks.setPlaceholder(translate("remarks.placeholder"));
        remarks.setHeight("200px");
        binder.forField(remarks).bind(RegistrationRecord::getRemarks, RegistrationRecord::setRemarks);
        formLayout.setColspan(remarks, 2);

        // Invitation Email Section
        var invitationEmailHeader = new H4(translate("invitation.email"));
        invitationEmailHeader.addClassName(LumoUtility.Margin.Top.LARGE);
        formLayout.setColspan(invitationEmailHeader, 4);

        var emailText = new TextArea(translate("email.text"));
        emailText.setPlaceholder(translate("email.text.placeholder"));
        emailText.setHeight("200px");
        binder.forField(emailText).bind(RegistrationRecord::getEmailText, RegistrationRecord::setEmailText);
        formLayout.setColspan(emailText, 2);

        // Confirmation Email Section
        var confirmationEmailHeader = new H4(translate("confirmation.email"));
        confirmationEmailHeader.addClassName(LumoUtility.Margin.Top.LARGE);
        formLayout.setColspan(confirmationEmailHeader, 4);

        var placeholdersHelp = new Span(translate("confirmation.email.placeholders.help"));
        placeholdersHelp.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        formLayout.setColspan(placeholdersHelp, 4);

        var confirmationSubjectNew = new TextField(translate("confirmation.email.subject.new"));
        binder.forField(confirmationSubjectNew)
            .bind(RegistrationRecord::getConfirmationEmailSubjectNew,
                    RegistrationRecord::setConfirmationEmailSubjectNew);
        formLayout.setColspan(confirmationSubjectNew, 2);

        var confirmationTextNew = new TextArea(translate("confirmation.email.text.new"));
        confirmationTextNew.setPlaceholder(translate("confirmation.email.text.placeholder"));
        confirmationTextNew.setHeight("200px");
        binder.forField(confirmationTextNew)
            .bind(RegistrationRecord::getConfirmationEmailTextNew, RegistrationRecord::setConfirmationEmailTextNew);
        formLayout.setColspan(confirmationTextNew, 2);

        var confirmationSubjectUpdate = new TextField(translate("confirmation.email.subject.update"));
        binder.forField(confirmationSubjectUpdate)
            .bind(RegistrationRecord::getConfirmationEmailSubjectUpdate,
                    RegistrationRecord::setConfirmationEmailSubjectUpdate);
        formLayout.setColspan(confirmationSubjectUpdate, 2);

        var confirmationTextUpdate = new TextArea(translate("confirmation.email.text.update"));
        confirmationTextUpdate.setPlaceholder(translate("confirmation.email.text.placeholder"));
        confirmationTextUpdate.setHeight("200px");
        binder.forField(confirmationTextUpdate)
            .bind(RegistrationRecord::getConfirmationEmailTextUpdate,
                    RegistrationRecord::setConfirmationEmailTextUpdate);
        formLayout.setColspan(confirmationTextUpdate, 2);

        formLayout.add(titleTextField, yearIntegerField, openFromDatePicker, openUntilDatePicker, remarks,
                invitationEmailHeader, emailText, confirmationEmailHeader, placeholdersHelp, confirmationSubjectNew,
                confirmationTextNew, confirmationSubjectUpdate, confirmationTextUpdate);

        editorDiv.add(formLayout);

        var listBoxFormLayout = new FormLayout();
        listBoxFormLayout.addClassName(LumoUtility.Padding.Top.LARGE);
        var eventsTitle = new H4(translate("events"));
        eventsTitle.addClassName(LumoUtility.Margin.Bottom.LARGE);
        var personsTitle = new H4(translate("persons"));
        personsTitle.addClassName(LumoUtility.Margin.Bottom.LARGE);
        listBoxFormLayout.add(eventsTitle, personsTitle);

        var selectAllEvents = new Button(translate("select.all.events"));
        selectAllEvents.addClickListener(e -> eventListBox.select(eventListBox.getListDataView().getItems().toList()));
        var selectNoEvents = new Button(translate("select.no.events"));
        selectNoEvents.addClickListener(e -> eventListBox.deselectAll());
        var eventButtons = new HorizontalLayout(selectAllEvents, selectNoEvents);

        var selectAllPersons = new Button(translate("select.all.persons"));
        selectAllPersons
            .addClickListener(e -> personListBox.select(personListBox.getListDataView().getItems().toList()));
        var selectNoPersons = new Button(translate("select.no.persons"));
        selectNoPersons.addClickListener(e -> personListBox.deselectAll());
        var personButtons = new HorizontalLayout(selectAllPersons, selectNoPersons);

        listBoxFormLayout.add(eventButtons, personButtons);

        eventListBox = new MultiSelectListBox<>();
        eventListBox.setId("event-list-box");
        eventListBox.addClassName(LumoUtility.Background.CONTRAST_10);
        eventListBox.setItemLabelGenerator(EventRecord::getTitle);
        eventListBox.addValueChangeListener(e -> setDirty(e.isFromClient()));

        var eventListBoxScroller = new Scroller(eventListBox);
        eventListBoxScroller.addClassName("scroller");
        eventListBoxScroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        eventListBoxScroller.setHeight("400px");

        personListBox = new MultiSelectListBox<>();
        personListBox.setId("person-list-box");
        personListBox.addClassName(LumoUtility.Background.CONTRAST_10);
        personListBox.setItemLabelGenerator(p -> "%s %s".formatted(p.getLastName(), p.getFirstName()));
        personListBox.addValueChangeListener(e -> setDirty(e.isFromClient()));

        var personListBoxScroller = new Scroller(personListBox);
        personListBoxScroller.addClassName("scroller");
        personListBoxScroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        personListBoxScroller.setHeight("400px");

        listBoxFormLayout.add(eventListBoxScroller, personListBoxScroller);

        editorDiv.add(listBoxFormLayout);

        createButtonLayout(editorLayoutDiv);

        return editorLayoutDiv;
    }

    private void setDirty(boolean fromClient) {
        if (fromClient) {
            dirty = true;
            setButtonState(grid.asSingleSelect().getValue());
        }
    }

    private void createButtonLayout(Div editorLayoutDiv) {
        var buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        createMailingButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        createMailingButton.setEnabled(false);
        sendEmailsButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        sendEmailsButton.setEnabled(false);
        buttonLayout.add(saveButton, cancelButton, createMailingButton, sendEmailsButton);

        editorLayoutDiv.add(buttonLayout);

        configureButtons();
    }

    private void configureButtons() {
        configureSaveButton();
        configureCancelButton();
        configureCreateMailingButton();
        configureSendMailsButton();
    }

    private void configureSendMailsButton() {
        sendEmailsButton.addClickListener(e -> {
            if (this.registration != null) {
                new ConfirmDialog(translate("send.emails"), translate("send.emails.confirm"), "Ja", confirmEvent -> {
                    registrationService.sendMails(this.registration, securityContext.getUsername());
                    Notification.success(translate("send.emails.success"));
                    refreshGridButPreserveSelection(this.registration.getId());
                }, translate(CANCEL), cancelEvent -> {
                }).open();
            }
        });
    }

    private void configureCreateMailingButton() {
        createMailingButton.addClickListener(e -> {
            if (this.registration != null) {
                new ConfirmDialog(translate(CREATE_MAILING), translate("create.mailing.confirm"), translate("yes"),
                        confirmEvent -> {
                            if (registrationService.createMailing(this.registration)) {
                                Notification.success(translate("create.mailing.success"));
                                refreshGridButPreserveSelection(this.registration.getId());
                            }
                            else {
                                Notification.error(getTranslation("create.mailing.error"));
                            }
                        }, translate(CANCEL), cancelEvent -> {
                        })
                    .open();
            }
        });
    }

    private void configureCancelButton() {
        cancelButton.addClickListener(e -> {
            clearForm();
            loadData();
        });
    }

    private void configureSaveButton() {
        saveButton.addClickListener(e -> {
            try {
                if (binder.validate().isOk()) {
                    binder.writeBean(this.registration);
                    registrationService.save(this.registration, this.eventListBox.getSelectedItems(),
                            this.personListBox.getSelectedItems());

                    refreshGridButPreserveSelection(this.registration.getId());

                    dirty = false;
                    setButtonState(grid.asSingleSelect().getValue());
                    Notification.success(translate("save.success"));
                }
            }
            catch (DataIntegrityViolationException | ValidationException dataIntegrityViolationException) {
                Notification.error(translate("save.error"));
            }
        });
    }

    private void refreshGridButPreserveSelection(long registrationId) {
        loadData();

        registrationRepository.findByIdFromView(registrationId).ifPresent(registrationViewRecord -> {
            grid.select(registrationViewRecord);
            setButtonState(registrationViewRecord);
        });
    }

    private void setButtonState(RegistrationViewRecord registrationViewRecord) {
        if (binder.hasChanges() || dirty) {
            createMailingButton.setEnabled(false);
            sendEmailsButton.setEnabled(false);
        }
        else {
            if (registrationViewRecord != null && registrationViewRecord.getId() != null) {

                createMailingButton.setText(registrationViewRecord.getEmailCreatedCount() > 0
                        ? translate("update.mailing") : translate(CREATE_MAILING));

                createMailingButton.setEnabled(
                        !eventListBox.getSelectedItems().isEmpty() && !personListBox.getSelectedItems().isEmpty());

                sendEmailsButton.setEnabled(registrationViewRecord.getEmailCreatedCount() > 0);
            }
        }
    }

    private void clearForm() {
        populateForm(null);
    }

    private void populateForm(RegistrationRecord value) {
        this.registration = value;
        binder.readBean(this.registration);

        personListBox
            .setItems(personRepository.findAll(PERSON.ACTIVE.isTrue(), List.of(PERSON.LAST_NAME, PERSON.FIRST_NAME)));

        if (value == null) {
            eventListBox.clear();
            personListBox.clear();

            enableComponents(false);
            cancelButton.setEnabled(false);
            saveButton.setEnabled(false);
        }
        else {
            if (this.registration.getId() != null) {
                loadEvents(this.registration.getYear());
                personListBox.setValue(new HashSet<>(personRepository.findByRegistrationId(this.registration.getId())));
                eventListBox.setValue(eventRepository.findByRegistrationId(this.registration.getId()));
            }

            enableComponents(true);
            cancelButton.setEnabled(true);
            saveButton.setEnabled(true);
        }
    }

    private void enableComponents(boolean enable) {
        formLayout.getChildren()
            .filter(HasEnabled.class::isInstance)
            .map(HasEnabled.class::cast)
            .forEach(hasEnabled -> hasEnabled.setEnabled(enable));
    }

    private void loadEvents(Integer year) {
        var fromDate = LocalDate.of(year, 1, 1);
        var toDate = LocalDate.of(year, 12, 31);

        eventListBox.setItems(eventRepository.findAll(EVENT.FROM_DATE.between(fromDate, toDate), List.of(EVENT.TITLE)));
    }

    @Override
    public String getPageTitle() {
        return translate("invitations");
    }

}
