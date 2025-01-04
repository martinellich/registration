package ch.martinelli.oss.registration.ui.views.persons;

import ch.martinelli.oss.registration.db.tables.records.PersonRecord;
import ch.martinelli.oss.registration.domain.PersonRepository;
import ch.martinelli.oss.registration.ui.components.I18nDatePicker;
import ch.martinelli.oss.registration.ui.components.Notification;
import ch.martinelli.oss.registration.ui.views.EditView;
import ch.martinelli.oss.vaadinjooq.util.VaadinJooqUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.dao.DataIntegrityViolationException;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.util.Optional;

import static ch.martinelli.oss.registration.db.tables.Person.PERSON;

@PageTitle("Jugeler")
@Route("persons/:personID?/:action?(edit)")
@Menu(order = 4, icon = LineAwesomeIconUrl.USERS_SOLID)
@RolesAllowed("ADMIN")
public class PersonsView extends EditView<PersonRecord> implements BeforeEnterObserver {

    public static final String PERSON_ID = "personID";
    private static final String PERSON_EDIT_ROUTE_TEMPLATE = "persons/%s/edit";

    private final transient PersonRepository personRepository;

    public PersonsView(PersonRepository personRepository) {
        this.personRepository = personRepository;

        grid = new Grid<>(PersonRecord.class, false);
        binder = new Binder<>(PersonRecord.class);

        SplitLayout splitLayout = new SplitLayout();
        splitLayout.addToPrimary(createGridLayout());
        splitLayout.addToSecondary(createEditorLayout());

        add(splitLayout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> personId = event.getRouteParameters().get(PERSON_ID).map(Long::parseLong);
        if (personId.isPresent()) {
            Optional<PersonRecord> personFromBackend = personRepository.findById(personId.get());
            if (personFromBackend.isPresent()) {
                populateForm(personFromBackend.get());
            } else {
                // when a row is selected but the data is no longer available, refresh grid
                grid.getDataProvider().refreshAll();
                event.forwardTo(PersonsView.class);
            }
        }
    }

    protected void configureGrid() {
        // Configure Grid
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        grid.addColumn(PersonRecord::getLastName)
                .setSortable(true).setSortProperty(PERSON.LAST_NAME.getName())
                .setHeader("Nachname").setAutoWidth(true);
        grid.addColumn(PersonRecord::getFirstName)
                .setSortable(true).setSortProperty(PERSON.FIRST_NAME.getName())
                .setHeader("Vornamen").setAutoWidth(true);
        grid.addColumn(PersonRecord::getEmail)
                .setSortable(true).setSortProperty(PERSON.EMAIL.getName())
                .setHeader("Email").setAutoWidth(true);
        grid.addColumn(PersonRecord::getDateOfBirth)
                .setSortable(true).setSortProperty(PERSON.DATE_OF_BIRTH.getName())
                .setHeader("Geburtsdatum").setAutoWidth(true);
        grid.addComponentColumn(personRecord ->
                        personRecord.getActive() != null && personRecord.getActive() ? VaadinIcon.CHECK.create() : new Span())
                .setSortable(true).setSortProperty(PERSON.ACTIVE.getName())
                .setHeader("Aktiv?").setAutoWidth(true);

        Button addButton = new Button(VaadinIcon.PLUS.create());
        addButton.setId("add-person-button");
        addButton.addClickListener(e -> {
            grid.deselectAll();
            grid.getDataProvider().refreshAll();
            PersonRecord personRecord = new PersonRecord();
            personRecord.setActive(true);
            populateForm(personRecord);
        });

        grid.addComponentColumn(personRecord -> {
            Button deleteButton = new Button(VaadinIcon.TRASH.create());
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(e ->
                    new ConfirmDialog("Person löschen",
                            "Willst du die Person wirklich löschen?",
                            "Ja",
                            ce -> {
                                try {
                                    personRepository.delete(personRecord);

                                    clearForm();
                                    grid.getDataProvider().refreshAll();

                                    Notification.success("Die Person wurde gelöscht");
                                } catch (DataIntegrityViolationException ex) {
                                    Notification.error("Die Person wird noch verwendet und kann nicht gelöscht werden");
                                }
                            },
                            "Abbrechen",
                            ce -> {
                            }).open());
            return deleteButton;
        }).setHeader(addButton).setTextAlign(ColumnTextAlign.END).setKey("action-column");


        grid.setItems(query -> personRepository.findAll(
                        query.getOffset(), query.getLimit(),
                        VaadinJooqUtil.orderFields(PERSON, query))
                .stream());

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate(String.format(PERSON_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                clearForm();
                UI.getCurrent().navigate(PersonsView.class);
            }
        });
    }

    protected void createComponents(FormLayout formLayout) {
        TextField lastNameTextField = new TextField("Nachname");
        binder.forField(lastNameTextField)
                .asRequired()
                .bind(PersonRecord::getLastName, PersonRecord::setLastName);

        TextField firstNameTextField = new TextField("Vorname");
        binder.forField(firstNameTextField)
                .asRequired()
                .bind(PersonRecord::getFirstName, PersonRecord::setFirstName);

        EmailField emailTextField = new EmailField("Email");
        binder.forField(emailTextField)
                .asRequired()
                .bind(PersonRecord::getEmail, PersonRecord::setEmail);

        I18nDatePicker dateOfBirthDatePicker = new I18nDatePicker("Geburtsdatum");
        binder.forField(dateOfBirthDatePicker)
                .asRequired()
                .bind(PersonRecord::getDateOfBirth, PersonRecord::setDateOfBirth);

        Checkbox active = new Checkbox("Aktiv");
        active.getElement().getThemeList().add("switch");
        binder.forField(active)
                .bind(PersonRecord::getActive, PersonRecord::setActive);

        formLayout.add(lastNameTextField, firstNameTextField, emailTextField, dateOfBirthDatePicker, active);
    }

    protected void configureButtons() {
        cancel.addClickListener(e -> {
            clearForm();
            grid.getDataProvider().refreshAll();
        });

        save.addClickListener(e -> {
            try {
                if (this.currentRecord == null) {
                    this.currentRecord = new PersonRecord();
                    this.currentRecord.setActive(true);
                }
                if (binder.validate().isOk()) {
                    boolean isNew = this.currentRecord.getId() == null;

                    binder.writeBean(this.currentRecord);
                    personRepository.save(this.currentRecord);

                    if (isNew) {
                        grid.getDataProvider().refreshAll();
                    } else {
                        grid.getDataProvider().refreshItem(this.currentRecord);
                    }

                    Notification.success("Die Daten wurden gespeichert");
                    UI.getCurrent().navigate(PersonsView.class);
                }
            } catch (DataIntegrityViolationException | ValidationException dataIntegrityViolationException) {
                Notification.error("Fehler beim Aktualisieren der Daten. Überprüfen Sie, ob alle Werte gültig sind");
            }
        });
    }

}
