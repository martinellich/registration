package ch.martinelli.oss.registration.ui.views.registration;

import ch.martinelli.oss.registration.db.tables.Registration;
import ch.martinelli.oss.registration.db.tables.records.RegistrationViewRecord;
import ch.martinelli.oss.registration.domain.RegistrationRepository;
import ch.martinelli.oss.vaadinjooq.util.VaadinJooqUtil;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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

import static ch.martinelli.oss.registration.db.tables.EventRegistration.EVENT_REGISTRATION;

@PageTitle("Anmeldungen")
@Route("event-registrations")
@RouteAlias("")
@Menu(order = 1, icon = LineAwesomeIconUrl.FILTER_SOLID)
@RolesAllowed("ADMIN")
@Uses(Icon.class)
public class EventRegistrationView extends Div {

    private Grid<RegistrationViewRecord> grid;

    private final Filters filters;
    private final RegistrationRepository registrationRepository;

    public EventRegistrationView(RegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;

        setSizeFull();
        addClassNames("event-registrations-view");

        filters = new Filters(this::refreshGrid);
        VerticalLayout layout = new VerticalLayout(createMobileFilters(), filters, createGrid());
        layout.setSizeFull();
        layout.setPadding(false);
        layout.setSpacing(false);
        add(layout);
    }

    private HorizontalLayout createMobileFilters() {
        // Mobile version
        HorizontalLayout mobileFilters = new HorizontalLayout();
        mobileFilters.setWidthFull();
        mobileFilters.addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.BoxSizing.BORDER,
                LumoUtility.AlignItems.CENTER);
        mobileFilters.addClassName("mobile-filters");

        Icon mobileIcon = new Icon("lumo", "plus");
        Span filtersHeading = new Span("Filters");
        mobileFilters.add(mobileIcon, filtersHeading);
        mobileFilters.setFlexGrow(1, filtersHeading);
        mobileFilters.addClickListener(e -> {
            if (filters.getClassNames().contains("visible")) {
                filters.removeClassName("visible");
                mobileIcon.getElement().setAttribute("icon", "lumo:plus");
            } else {
                filters.addClassName("visible");
                mobileIcon.getElement().setAttribute("icon", "lumo:minus");
            }
        });
        return mobileFilters;
    }

    public static class Filters extends Div {

        private final TextField name = new TextField("Name");

        public Filters(Runnable onSearch) {

            setWidthFull();
            addClassName("filter-layout");
            addClassNames(LumoUtility.Padding.Horizontal.LARGE, LumoUtility.Padding.Vertical.MEDIUM,
                    LumoUtility.BoxSizing.BORDER);
            name.setPlaceholder("First or last name");

            // Action buttons
            Button resetBtn = new Button("Reset");
            resetBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            resetBtn.addClickListener(e -> {
                name.clear();
                onSearch.run();
            });
            Button searchBtn = new Button("Search");
            searchBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            searchBtn.addClickListener(e -> onSearch.run());

            Div actions = new Div(resetBtn, searchBtn);
            actions.addClassName(LumoUtility.Gap.SMALL);
            actions.addClassName("actions");

            add(name, actions);
        }

        public Condition toCondition() {
            Condition condition = DSL.noCondition();

            if (!name.isEmpty()) {
                condition.and(EVENT_REGISTRATION.person().FIRST_NAME.likeIgnoreCase(name.getValue())
                        .or(EVENT_REGISTRATION.person().LAST_NAME.likeIgnoreCase(name.getValue())));
            }
            return condition;
        }
    }

    private Component createGrid() {
        grid = new Grid<>(RegistrationViewRecord.class, false);
        grid.addColumn(RegistrationViewRecord::getTitle).setAutoWidth(true);
        grid.addColumn(RegistrationViewRecord::getLocation).setAutoWidth(true);
        grid.addColumn(RegistrationViewRecord::getFromDate).setAutoWidth(true);
        grid.addColumn(RegistrationViewRecord::getLastName).setAutoWidth(true);
        grid.addColumn(RegistrationViewRecord::getFirstName).setAutoWidth(true);

        grid.setItems(query -> registrationRepository.findAllFromView(
                filters.toCondition(),
                query.getOffset(), query.getLimit(),
                VaadinJooqUtil.orderFields(Registration.REGISTRATION, query)
        ).stream());
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.addClassNames(LumoUtility.Border.TOP, LumoUtility.BorderColor.CONTRAST_10);

        return grid;
    }

    private void refreshGrid() {
        grid.getDataProvider().refreshAll();
    }

}
