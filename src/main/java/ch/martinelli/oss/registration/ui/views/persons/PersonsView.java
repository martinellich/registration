package ch.martinelli.oss.registration.ui.views.persons;

import ch.martinelli.oss.registration.db.tables.Person;
import ch.martinelli.oss.registration.db.tables.records.PersonRecord;
import ch.martinelli.oss.registration.domain.ExcelPersonParser;
import ch.martinelli.oss.registration.domain.PersonChangeDetector;
import ch.martinelli.oss.registration.domain.PersonRepository;
import ch.martinelli.oss.registration.security.Roles;
import ch.martinelli.oss.registration.ui.components.I18nDatePicker;
import ch.martinelli.oss.registration.ui.components.Icon;
import ch.martinelli.oss.registration.ui.components.Notification;
import ch.martinelli.oss.registration.ui.views.EditView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.dao.DataIntegrityViolationException;
import org.vaadin.lineawesome.LineAwesomeIcon;

import static ch.martinelli.oss.registration.db.tables.Person.PERSON;
import static com.vaadin.flow.i18n.I18NProvider.translate;

@SuppressWarnings("java:S110")
@RolesAllowed({ Roles.USER, Roles.ADMIN })
@Route("persons/:" + EditView.ID + "?")
public class PersonsView extends EditView<Person, PersonRecord, PersonRepository>
        implements BeforeEnterObserver, HasDynamicTitle {

    private boolean hideInactive;

    private final transient ExcelPersonParser excelPersonParser;

    private final transient PersonChangeDetector personChangeDetector;

    public PersonsView(PersonRepository personRepository, ExcelPersonParser excelPersonParser,
            PersonChangeDetector personChangeDetector) {
        super(personRepository, PERSON, new Grid<>(PersonRecord.class, false), new Binder<>(PersonRecord.class));

        this.excelPersonParser = excelPersonParser;
        this.personChangeDetector = personChangeDetector;
        this.hideInactive = true; // Initialize in constructor
        afterNewRecord = personRecord -> personRecord.setActive(true); // default value
    }

    @Override
    public String getPageTitle() {
        return translate("persons");
    }

    @Override
    protected Div createGridLayout() {
        var wrapper = new Div();
        wrapper.setClassName("grid-wrapper");

        // Add toolbar with filter button
        var toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        toolbar.setPadding(true);
        toolbar.setSpacing(true);

        // Upload button for Excel import
        var uploadButton = createUploadButton();

        // When hideInactive is true (default), button should show "Show inactive" action
        // Note: field may not be initialized yet if called from parent constructor
        var toggleInactiveButton = new Button(translate("show.inactive"));
        toggleInactiveButton.setId("toggle-inactive-button");
        toggleInactiveButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        toggleInactiveButton.addClickListener(event -> {
            hideInactive = !hideInactive;
            toggleInactiveButton.setText(hideInactive ? translate("show.inactive") : translate("hide.inactive"));
            grid.getDataProvider().refreshAll();
        });

        toolbar.add(uploadButton, toggleInactiveButton);

        wrapper.add(toolbar, grid);

        configureGrid();
        addActionColumn();
        addSelectionListener();
        setItems();

        return wrapper;
    }

    protected void configureGrid() {
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        var lastNameColumn = grid.addColumn(PersonRecord::getLastName)
            .setSortable(true)
            .setSortProperty(PERSON.LAST_NAME.getName())
            .setHeader(translate("last.name"))
            .setAutoWidth(true);
        var firstNameColumn = grid.addColumn(PersonRecord::getFirstName)
            .setSortable(true)
            .setSortProperty(PERSON.FIRST_NAME.getName())
            .setHeader(translate("first.name"))
            .setAutoWidth(true);
        grid.addColumn(PersonRecord::getEmail)
            .setSortable(true)
            .setSortProperty(PERSON.EMAIL.getName())
            .setHeader(translate("email"))
            .setAutoWidth(true);
        grid.addColumn(PersonRecord::getDateOfBirth)
            .setSortable(true)
            .setSortProperty(PERSON.DATE_OF_BIRTH.getName())
            .setHeader(translate("date.of.birth"))
            .setAutoWidth(true);
        grid.addComponentColumn(personRecord -> personRecord.getActive() != null && personRecord.getActive()
                ? LineAwesomeIcon.CHECK_SOLID.create() : new Span())
            .setSortable(true)
            .setSortProperty(PERSON.ACTIVE.getName())
            .setHeader(translate("active"))
            .setAutoWidth(true);

        grid.sort(GridSortOrder.asc(lastNameColumn).thenAsc(firstNameColumn).build());
    }

    @Override
    protected void setItems() {
        grid.setItems(query -> repository
            .findAll(query.getOffset(), query.getLimit(),
                    ch.martinelli.oss.vaadinjooq.util.VaadinJooqUtil.orderFields(PERSON, query))
            .stream()
            .filter(person -> !hideInactive || Boolean.TRUE.equals(person.getActive())));
    }

    protected void createComponents(FormLayout formLayout) {
        var lastNameTextField = new TextField(translate("last.name"));
        binder.forField(lastNameTextField).asRequired().bind(PersonRecord::getLastName, PersonRecord::setLastName);

        var firstNameTextField = new TextField(translate("first.name"));
        binder.forField(firstNameTextField).asRequired().bind(PersonRecord::getFirstName, PersonRecord::setFirstName);

        var emailTextField = new EmailField(translate("email"));
        binder.forField(emailTextField).bind(PersonRecord::getEmail, PersonRecord::setEmail);

        var dateOfBirthDatePicker = new I18nDatePicker(translate("date.of.birth"));
        binder.forField(dateOfBirthDatePicker).bind(PersonRecord::getDateOfBirth, PersonRecord::setDateOfBirth);

        var active = new Checkbox(translate("active"));
        active.getElement().getThemeList().add("switch");
        binder.forField(active).bind(PersonRecord::getActive, PersonRecord::setActive);

        formLayout.add(lastNameTextField, firstNameTextField, emailTextField, dateOfBirthDatePicker, active);
    }

    @Override
    protected void addActionColumn() {
        var addIcon = new Icon(LineAwesomeIcon.PLUS_CIRCLE_SOLID, e -> {
            grid.deselectAll();
            grid.getDataProvider().refreshAll();
            var personRecord = PERSON.newRecord();
            if (afterNewRecord != null) {
                afterNewRecord.accept(personRecord);
            }
            populateForm(personRecord);
        });
        addIcon.setId("add-icon");
        addIcon.addClassName(ACTION_ICON);

        grid.addComponentColumn(personRecord -> {
            var deleteIcon = new Icon(LineAwesomeIcon.TRASH_SOLID, e -> new ConfirmDialog(translate("delete.record"),
                    translate("delete.record.question"), translate("yes"), ce -> {
                        try {
                            repository.delete(personRecord);

                            clearForm();
                            grid.getDataProvider().refreshAll();

                            Notification.success(translate("delete.record.success"));
                        }
                        catch (DataIntegrityViolationException ex) {
                            // Person cannot be deleted due to foreign key constraints,
                            // deactivate instead
                            personRecord.setActive(false);
                            repository.save(personRecord);

                            clearForm();
                            grid.getDataProvider().refreshAll();

                            Notification.success(translate("deactivate.record.success"));
                        }
                    }, translate("cancel"), ce -> {
                    })
                .open());
            deleteIcon.addClassName("delete-icon");
            return deleteIcon;
        }).setHeader(addIcon).setTextAlign(ColumnTextAlign.END).setKey("action-column");
    }

    private Button createUploadButton() {
        Button uploadButton = new Button(translate("upload.persons"));
        uploadButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        uploadButton.addClickListener(event -> {
            PersonUploadDialog dialog = new PersonUploadDialog(excelPersonParser, personChangeDetector, repository,
                    () -> {
                        grid.getDataProvider().refreshAll();
                        clearForm();
                    });
            dialog.open();
        });
        return uploadButton;
    }

}
