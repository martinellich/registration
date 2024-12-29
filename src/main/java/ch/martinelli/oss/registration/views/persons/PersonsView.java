package ch.martinelli.oss.registration.views.persons;

import ch.martinelli.oss.registration.db.tables.Person;
import ch.martinelli.oss.registration.db.tables.records.PersonRecord;
import ch.martinelli.oss.registration.domain.PersonRepository;
import ch.martinelli.oss.vaadinjooq.util.VaadinJooqUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.RolesAllowed;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.util.Optional;

@PageTitle("Jugeler")
@Route("persons/:personID?/:action?(edit)")
@Menu(order = 3, icon = LineAwesomeIconUrl.USERS_SOLID)
@RolesAllowed("ADMIN")
public class PersonsView extends Div implements BeforeEnterObserver {

    private final String PERSON_ID = "personID";
    private final String PERSON_EDIT_ROUTE_TEMPLATE = "persons/%s/edit";

    private final Grid<PersonRecord> grid = new Grid<>(PersonRecord.class, false);

    private final Button cancel = new Button("Cancel");
    private final Button save = new Button("Save");

    private final Binder<PersonRecord> binder = new Binder<>(PersonRecord.class);

    private PersonRecord person;

    private final PersonRepository personRepository;

    public PersonsView(PersonRepository personRepository) {
        this.personRepository = personRepository;
        addClassNames("persons-view");

        // Create UI
        SplitLayout splitLayout = new SplitLayout();

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        add(splitLayout);

        // Configure Grid
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        grid.addColumn(PersonRecord::getLastName).setHeader("Nachname").setAutoWidth(true);
        grid.addColumn(PersonRecord::getFirstName).setHeader("Vornamen").setAutoWidth(true);
        grid.addColumn(PersonRecord::getEmail).setHeader("Email").setAutoWidth(true);
        grid.addColumn(PersonRecord::getDateOfBirth).setHeader("Geburtsdatum").setAutoWidth(true);

        grid.setItems(query -> personRepository.findAll(
                        query.getOffset(), query.getLimit(),
                        VaadinJooqUtil.orderFields(Person.PERSON, query))
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

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
        });

        save.addClickListener(e -> {
            try {
                if (this.person == null) {
                    this.person = new PersonRecord();
                }
                binder.writeBean(this.person);
                personRepository.save(this.person);
                clearForm();
                refreshGrid();
                Notification.show("Data updated");
                UI.getCurrent().navigate(PersonsView.class);
            } catch (ValidationException validationException) {
                Notification.show("Failed to update the data. Check again that all values are valid");
            }
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> personId = event.getRouteParameters().get(PERSON_ID).map(Long::parseLong);
        if (personId.isPresent()) {
            Optional<PersonRecord> personFromBackend = personRepository.findById(personId.get());
            if (personFromBackend.isPresent()) {
                populateForm(personFromBackend.get());
            } else {
                Notification.show(String.format("The requested person was not found, ID = %s", personId.get()), 3000,
                        Notification.Position.BOTTOM_START);
                // when a row is selected but the data is no longer available,
                // refresh grid
                refreshGrid();
                event.forwardTo(PersonsView.class);
            }
        }
    }

    private void createEditorLayout(SplitLayout splitLayout) {
        Div editorLayoutDiv = new Div();
        editorLayoutDiv.setClassName("editor-layout");

        Div editorDiv = new Div();
        editorDiv.setClassName("editor");
        editorLayoutDiv.add(editorDiv);

        FormLayout formLayout = new FormLayout();

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

        DatePicker dateOfBirthDatePicker = new DatePicker("Geburtsdatum");
        binder.forField(dateOfBirthDatePicker)
                .asRequired()
                .bind(PersonRecord::getDateOfBirth, PersonRecord::setDateOfBirth);

        formLayout.add(lastNameTextField, firstNameTextField, emailTextField, dateOfBirthDatePicker);

        editorDiv.add(formLayout);
        createButtonLayout(editorLayoutDiv);

        splitLayout.addToSecondary(editorLayoutDiv);
    }

    private void createButtonLayout(Div editorLayoutDiv) {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonLayout.add(save, cancel);
        editorLayoutDiv.add(buttonLayout);
    }

    private void createGridLayout(SplitLayout splitLayout) {
        Div wrapper = new Div();
        wrapper.setClassName("grid-wrapper");
        splitLayout.addToPrimary(wrapper);
        wrapper.add(grid);
    }

    private void refreshGrid() {
        grid.select(null);
        grid.getDataProvider().refreshAll();
    }

    private void clearForm() {
        populateForm(null);
    }

    private void populateForm(PersonRecord value) {
        this.person = value;
        binder.readBean(this.person);

    }
}
