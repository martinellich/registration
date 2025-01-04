package ch.martinelli.oss.registration.ui.views.registration;

import ch.martinelli.oss.registration.db.tables.Registration;
import ch.martinelli.oss.registration.db.tables.records.RegistrationEmailViewRecord;
import ch.martinelli.oss.registration.db.tables.records.RegistrationRecord;
import ch.martinelli.oss.registration.domain.RegistrationEmailRepository;
import ch.martinelli.oss.registration.domain.RegistrationRepository;
import ch.martinelli.oss.registration.ui.components.DateFormat;
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
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
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

    private final Select<RegistrationRecord> registrationSelect = new Select<>();
    private final RegistrationRepository registrationRepository;

    public RegistrationEmailView(RegistrationEmailRepository registrationEmailRepository, RegistrationRepository registrationRepository) {
        this.registrationEmailRepository = registrationEmailRepository;
        this.registrationRepository = registrationRepository;

        setSizeFull();

        add(createFilters(), createGrid());
    }

    public FormLayout createFilters() {
        registrationSelect.setLabel("Jahr");
        registrationSelect.setItemLabelGenerator(r -> r.getYear().toString());
        registrationSelect.setItems(registrationRepository.findAll(DSL.noCondition()));

        Button searchButton = new Button("Suchen");
        searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        searchButton.addClickListener(e -> grid.getDataProvider().refreshAll());

        Button resetButton = new Button("Reset");
        resetButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        resetButton.addClickListener(e -> registrationSelect.clear());

        FormLayout formLayout = new FormLayout(registrationSelect, new HorizontalLayout(searchButton, resetButton));
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
        grid.addColumn(registrationEmailViewRecord -> DateFormat.DATE_TIME_FORMAT.format(registrationEmailViewRecord.getSentAt()))
                .setSortable(true).setSortProperty(REGISTRATION_EMAIL_VIEW.SENT_AT.getName())
                .setHeader("Versendet").setAutoWidth(true);
        grid.addComponentColumn(registrationEmailViewRecord -> {
            RouterLink link = new RouterLink("Anmeldeformular", PublicEventRegistrationView.class, registrationEmailViewRecord.getLink());
            link.getElement().setAttribute("onclick", "window.open(this.href, '_blank'); return false;");
            return link;
        });

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

        if (registrationSelect.getValue() != null) {
            condition = condition.and(REGISTRATION_EMAIL_VIEW.YEAR.eq(registrationSelect.getValue().getYear()));
        }
        return condition;
    }

}
