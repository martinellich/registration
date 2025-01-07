package ch.martinelli.oss.registration.ui.views.persons;

import ch.martinelli.oss.registration.db.tables.Person;
import ch.martinelli.oss.registration.db.tables.records.PersonRecord;
import ch.martinelli.oss.registration.domain.PersonRepository;
import ch.martinelli.oss.registration.ui.components.I18nDatePicker;
import ch.martinelli.oss.registration.ui.views.EditView;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import static ch.martinelli.oss.registration.db.tables.Person.PERSON;
import static com.vaadin.flow.i18n.I18NProvider.translate;

@SuppressWarnings("java:S110")
@Route("persons/:id?")
@RolesAllowed("ADMIN")
public class PersonsView extends EditView<Person, PersonRecord, PersonRepository> implements BeforeEnterObserver, HasDynamicTitle {

    public PersonsView(PersonRepository personRepository) {
        super(personRepository, PERSON, new Grid<>(PersonRecord.class, false), new Binder<>(PersonRecord.class));

        afterNewRecord = personRecord -> personRecord.setActive(true); // default value
    }

    @Override
    public String getPageTitle() {
        return translate("persons");
    }

    protected void configureGrid() {
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        grid.addColumn(PersonRecord::getLastName)
                .setSortable(true).setSortProperty(PERSON.LAST_NAME.getName())
                .setHeader(translate("last.name")).setAutoWidth(true);
        grid.addColumn(PersonRecord::getFirstName)
                .setSortable(true).setSortProperty(PERSON.FIRST_NAME.getName())
                .setHeader(translate("first.name")).setAutoWidth(true);
        grid.addColumn(PersonRecord::getEmail)
                .setSortable(true).setSortProperty(PERSON.EMAIL.getName())
                .setHeader(translate("email")).setAutoWidth(true);
        grid.addColumn(PersonRecord::getDateOfBirth)
                .setSortable(true).setSortProperty(PERSON.DATE_OF_BIRTH.getName())
                .setHeader(translate("date.of.birth")).setAutoWidth(true);
        grid.addComponentColumn(personRecord ->
                        personRecord.getActive() != null && personRecord.getActive() ? VaadinIcon.CHECK.create() : new Span())
                .setSortable(true).setSortProperty(PERSON.ACTIVE.getName())
                .setHeader(translate("active")).setAutoWidth(true);
    }

    protected void createComponents(FormLayout formLayout) {
        TextField lastNameTextField = new TextField(translate("last.name"));
        binder.forField(lastNameTextField)
                .asRequired()
                .bind(PersonRecord::getLastName, PersonRecord::setLastName);

        TextField firstNameTextField = new TextField(translate("first.name"));
        binder.forField(firstNameTextField)
                .asRequired()
                .bind(PersonRecord::getFirstName, PersonRecord::setFirstName);

        EmailField emailTextField = new EmailField(translate("email"));
        binder.forField(emailTextField)
                .bind(PersonRecord::getEmail, PersonRecord::setEmail);

        I18nDatePicker dateOfBirthDatePicker = new I18nDatePicker(translate("date.of.birth"));
        binder.forField(dateOfBirthDatePicker)
                .bind(PersonRecord::getDateOfBirth, PersonRecord::setDateOfBirth);

        Checkbox active = new Checkbox(translate("active"));
        active.getElement().getThemeList().add("switch");
        binder.forField(active)
                .bind(PersonRecord::getActive, PersonRecord::setActive);

        formLayout.add(lastNameTextField, firstNameTextField, emailTextField, dateOfBirthDatePicker, active);
    }

}
