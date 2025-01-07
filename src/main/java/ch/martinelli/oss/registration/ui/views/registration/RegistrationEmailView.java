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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.jooq.Condition;
import org.jooq.impl.DSL;

import static ch.martinelli.oss.registration.db.tables.RegistrationEmailView.REGISTRATION_EMAIL_VIEW;
import static com.vaadin.flow.i18n.I18NProvider.translate;

@Route("registration-emails")
@RolesAllowed("USER")
@Uses(Icon.class)
public class RegistrationEmailView extends Div implements HasDynamicTitle {

    private final transient RegistrationEmailRepository registrationEmailRepository;
    private final transient RegistrationRepository registrationRepository;

    private final Grid<RegistrationEmailViewRecord> grid = new Grid<>(RegistrationEmailViewRecord.class, false);

    private final Select<RegistrationRecord> registrationSelect = new Select<>();

    public RegistrationEmailView(RegistrationEmailRepository registrationEmailRepository, RegistrationRepository registrationRepository) {
        this.registrationEmailRepository = registrationEmailRepository;
        this.registrationRepository = registrationRepository;

        setSizeFull();

        add(createFilter(), createGrid());
    }

    public VerticalLayout createFilter() {
        registrationSelect.setLabel(translate("invitation"));
        registrationSelect.setItemLabelGenerator(r -> "%s %s".formatted(r.getTitle(), r.getYear().toString()));
        registrationSelect.setItems(registrationRepository.findAll(DSL.noCondition()));
        registrationSelect.addValueChangeListener(e -> grid.getDataProvider().refreshAll());

        Button resetButton = new Button(translate("reset"));
        resetButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        resetButton.addClickListener(e -> {
            registrationSelect.clear();
            grid.getDataProvider().refreshAll();
        });

        FormLayout formLayout = new FormLayout(registrationSelect, new HorizontalLayout(resetButton));
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        return new VerticalLayout(formLayout);
    }

    private Component createGrid() {
        grid.setHeight("calc(100% - 100px)");
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.addClassNames(LumoUtility.Border.TOP, LumoUtility.BorderColor.CONTRAST_10);
        grid.setEmptyStateText(translate("no.data"));

        grid.addColumn(RegistrationEmailViewRecord::getYear)
                .setSortable(true).setSortProperty(REGISTRATION_EMAIL_VIEW.YEAR.getName())
                .setHeader(translate("year")).setAutoWidth(true);
        grid.addColumn(RegistrationEmailViewRecord::getEmail)
                .setSortable(true).setSortProperty(REGISTRATION_EMAIL_VIEW.EMAIL.getName())
                .setHeader(translate("email")).setAutoWidth(true);
        grid.addColumn(RegistrationEmailViewRecord::getLink)
                .setSortable(true).setSortProperty(REGISTRATION_EMAIL_VIEW.LINK.getName())
                .setHeader(translate("link")).setAutoWidth(true);
        grid.addColumn(registrationEmailViewRecord -> registrationEmailViewRecord.getSentAt() != null
                        ? DateFormat.DATE_TIME_FORMAT.format(registrationEmailViewRecord.getSentAt()) : "")
                .setSortable(true).setSortProperty(REGISTRATION_EMAIL_VIEW.SENT_AT.getName())
                .setHeader(translate("sent")).setAutoWidth(true);
        grid.addComponentColumn(registrationEmailViewRecord -> {
            RouterLink link = new RouterLink(translate("registration.form"), PublicEventRegistrationView.class, registrationEmailViewRecord.getLink());
            link.getElement().setAttribute("onclick", "window.open(this.href, '_blank'); return false;");
            return link;
        });

        grid.setItems(query -> registrationEmailRepository.findAllFromView(
                getFilter(),
                query.getOffset(), query.getLimit(),
                VaadinJooqUtil.orderFields(Registration.REGISTRATION, query)
        ).stream());

        return grid;
    }

    private Condition getFilter() {
        Condition condition = DSL.falseCondition();

        if (registrationSelect.getValue() != null) {
            condition = REGISTRATION_EMAIL_VIEW.YEAR.eq(registrationSelect.getValue().getYear());
        }
        return condition;
    }

    @Override
    public String getPageTitle() {
        return translate("mailing");
    }
}
