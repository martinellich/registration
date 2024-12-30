package ch.martinelli.oss.registration.ui.views.registration;

import ch.martinelli.oss.registration.db.tables.Registration;
import ch.martinelli.oss.registration.db.tables.records.EventRegistrationViewRecord;
import ch.martinelli.oss.registration.domain.RegistrationRepository;
import ch.martinelli.oss.vaadinjooq.util.VaadinJooqUtil;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import static ch.martinelli.oss.registration.db.tables.EventRegistrationView.EVENT_REGISTRATION_VIEW;

@PageTitle("Anmeldungen")
@Route("event-registrations")
@RouteAlias("")
@Menu(order = 1, icon = LineAwesomeIconUrl.FILTER_SOLID)
@RolesAllowed("ADMIN")
@Uses(Icon.class)
public class EventRegistrationView extends VerticalLayout {

    private final transient RegistrationRepository registrationRepository;

    private final Grid<EventRegistrationViewRecord> grid = new Grid<>(EventRegistrationViewRecord.class, false);

    private TextField nameTextField;
    private IntegerField yearIntegerField;

    public EventRegistrationView(RegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;

        setSizeFull();

        add(createFilters(), createGrid());
    }

    public FormLayout createFilters() {
        nameTextField = new TextField();
        nameTextField.setPlaceholder("Vor- oder Nachname");

        yearIntegerField = new IntegerField();
        yearIntegerField.setPlaceholder("Jahr");

        Button searchButton = new Button("Suchen");
        searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        searchButton.addClickListener(e -> grid.getDataProvider().refreshAll());

        Button resetButton = new Button("Reset");
        resetButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        resetButton.addClickListener(e -> {
            nameTextField.clear();
            yearIntegerField.clear();
        });

        FormLayout formLayout = new FormLayout(nameTextField, yearIntegerField, new HorizontalLayout(searchButton, resetButton));
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 3));

        return formLayout;
    }

    private Component createGrid() {
        grid.addColumn(EventRegistrationViewRecord::getTitle)
                .setSortable(true).setSortProperty(EVENT_REGISTRATION_VIEW.TITLE.getName())
                .setHeader("Bezeichnung").setAutoWidth(true);
        grid.addColumn(EventRegistrationViewRecord::getLocation)
                .setSortable(true).setSortProperty(EVENT_REGISTRATION_VIEW.LOCATION.getName())
                .setHeader("Beschreibung").setAutoWidth(true);
        grid.addColumn(EventRegistrationViewRecord::getFromDate)
                .setSortable(true).setSortProperty(EVENT_REGISTRATION_VIEW.FROM_DATE.getName())
                .setHeader("Datum").setAutoWidth(true);
        grid.addColumn(EventRegistrationViewRecord::getLastName)
                .setSortable(true).setSortProperty(EVENT_REGISTRATION_VIEW.LAST_NAME.getName())
                .setHeader("Nachname").setAutoWidth(true);
        grid.addColumn(EventRegistrationViewRecord::getFirstName)
                .setSortable(true).setSortProperty(EVENT_REGISTRATION_VIEW.FIRST_NAME.getName())
                .setHeader("Vorname").setAutoWidth(true);

        grid.setItems(query -> registrationRepository.findAllEventRegistrationsFromView(
                getFilter(),
                query.getOffset(), query.getLimit(),
                VaadinJooqUtil.orderFields(Registration.REGISTRATION, query)
        ).stream());
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.addClassNames(LumoUtility.Border.TOP, LumoUtility.BorderColor.CONTRAST_10);

        return grid;
    }

    private Condition getFilter() {
        Condition condition = DSL.noCondition();

        if (!nameTextField.isEmpty()) {
            condition = condition.and(EVENT_REGISTRATION_VIEW.FIRST_NAME.likeIgnoreCase(nameTextField.getValue())
                    .or(EVENT_REGISTRATION_VIEW.LAST_NAME.likeIgnoreCase(nameTextField.getValue())));
        }
        if (!yearIntegerField.isEmpty()) {
            condition = condition.and(DSL.year(EVENT_REGISTRATION_VIEW.FROM_DATE).eq(yearIntegerField.getValue()));
        }
        return condition;
    }

}
