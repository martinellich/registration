package ch.martinelli.oss.registration.ui.views.registration;

import ch.martinelli.oss.registration.db.tables.Registration;
import ch.martinelli.oss.registration.db.tables.records.RegistrationEmailViewRecord;
import ch.martinelli.oss.registration.domain.RegistrationEmailRepository;
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
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import static ch.martinelli.oss.registration.db.tables.RegistrationEmailView.REGISTRATION_EMAIL_VIEW;

@PageTitle("Versand")
@Route("registration-emails")
@Menu(order = 1, icon = LineAwesomeIconUrl.MAIL_BULK_SOLID)
@RolesAllowed("ADMIN")
@Uses(Icon.class)
public class RegistrationEmailView extends VerticalLayout {

    private final transient RegistrationEmailRepository registrationEmailRepository;

    private final Grid<RegistrationEmailViewRecord> grid = new Grid<>(RegistrationEmailViewRecord.class, false);

    private IntegerField yearIntegerField;

    public RegistrationEmailView(RegistrationEmailRepository registrationEmailRepository) {
        this.registrationEmailRepository = registrationEmailRepository;

        setSizeFull();

        add(createFilters(), createGrid());
    }

    public FormLayout createFilters() {
        yearIntegerField = new IntegerField();
        yearIntegerField.setPlaceholder("Jahr");

        Button searchButton = new Button("Suchen");
        searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        searchButton.addClickListener(e -> grid.getDataProvider().refreshAll());

        Button resetButton = new Button("Reset");
        resetButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        resetButton.addClickListener(e -> yearIntegerField.clear());

        FormLayout formLayout = new FormLayout(yearIntegerField, new HorizontalLayout(searchButton, resetButton));
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        return formLayout;
    }

    private Component createGrid() {
        grid.addColumn(RegistrationEmailViewRecord::getYear)
                .setSortable(true).setSortProperty(REGISTRATION_EMAIL_VIEW.YEAR.getName())
                .setHeader("Jahr").setAutoWidth(true);
        grid.addColumn(RegistrationEmailViewRecord::getEmail)
                .setSortable(true).setSortProperty(REGISTRATION_EMAIL_VIEW.EMAIL.getName())
                .setHeader("Email").setAutoWidth(true);
        grid.addColumn(RegistrationEmailViewRecord::getLink)
                .setSortable(true).setSortProperty(REGISTRATION_EMAIL_VIEW.LINK.getName())
                .setHeader("Link").setAutoWidth(true);
        grid.addColumn(RegistrationEmailViewRecord::getSentAt)
                .setSortable(true).setSortProperty(REGISTRATION_EMAIL_VIEW.SENT_AT.getName())
                .setHeader("Versendet").setAutoWidth(true);

        grid.setItems(query -> registrationEmailRepository.findAllFromView(
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

        if (!yearIntegerField.isEmpty()) {
            condition = condition.and(REGISTRATION_EMAIL_VIEW.YEAR.eq(yearIntegerField.getValue()));
        }
        return condition;
    }

}
