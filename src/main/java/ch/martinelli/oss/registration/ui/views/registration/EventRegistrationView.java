package ch.martinelli.oss.registration.ui.views.registration;

import ch.martinelli.oss.registration.db.tables.records.RegistrationRecord;
import ch.martinelli.oss.registration.domain.EventRegistrationRepository;
import ch.martinelli.oss.registration.domain.EventRegistrationRow;
import ch.martinelli.oss.registration.domain.RegistrationRepository;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.*;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.jooq.impl.DSL;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.util.List;

@PageTitle("Anmeldungen")
@Route("event-registrations")
@Menu(order = 1, icon = LineAwesomeIconUrl.TH_LIST_SOLID)
@RolesAllowed("ADMIN")
@Uses(Icon.class)
public class EventRegistrationView extends Div implements HasUrlParameter<Long> {

    private final transient EventRegistrationRepository eventRegistrationRepository;
    private final transient RegistrationRepository registrationRepository;

    private final Select<RegistrationRecord> registrationSelect = new Select<>();
    private Div gridContainer;
    private Grid<EventRegistrationRow> grid;

    private Long registrationId;

    public EventRegistrationView(EventRegistrationRepository eventRegistrationRepository,
                                 RegistrationRepository registrationRepository) {
        this.eventRegistrationRepository = eventRegistrationRepository;
        this.registrationRepository = registrationRepository;

        addClassNames("event-registrations-view");
        setSizeFull();
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter Long registrationId) {
        this.registrationId = registrationId;

        removeAll();
        gridContainer = new Div();
        gridContainer.setHeightFull();
        add(createFilter(), gridContainer);
        createButtons();

        registrationSelect.setValue(registrationRepository.findById(registrationId).orElse(null));
    }

    public VerticalLayout createFilter() {
        registrationSelect.setLabel("Jahr");
        registrationSelect.setItemLabelGenerator(r -> r.getYear().toString());
        registrationSelect.setItems(registrationRepository.findAll(DSL.noCondition()));
        registrationSelect.addValueChangeListener(e -> {
            if (registrationSelect.getValue() != null) {
                this.registrationId = registrationSelect.getValue().getId();
                createGrid();
            }
        });

        return new VerticalLayout(registrationSelect);
    }

    private void createGrid() {
        gridContainer.removeAll();

        List<EventRegistrationRow> eventRegistrationMatrix = eventRegistrationRepository.getEventRegistrationMatrix(registrationId);

        grid = new Grid<>(EventRegistrationRow.class, false);
        grid.addColumn(EventRegistrationRow::lastName)
                .setHeader("Nachname").setAutoWidth(true);
        grid.addColumn(EventRegistrationRow::firstName)
                .setHeader("Vorname").setAutoWidth(true);

        if (!eventRegistrationMatrix.isEmpty()) {
            EventRegistrationRow firstRow = eventRegistrationMatrix.getFirst();
            firstRow.registrations().forEach((event, r) ->
                    grid.addComponentColumn(registrationRow -> {
                                boolean registered = registrationRow.registrations().get(event);
                                if (registered) {
                                    return VaadinIcon.CHECK.create();
                                } else {
                                    return new Span();
                                }
                            })
                            .setHeader(event).setWidth("20px"));
        }

        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.addClassNames(LumoUtility.Border.TOP, LumoUtility.BorderColor.CONTRAST_10);

        grid.setItems(eventRegistrationMatrix);

        gridContainer.add(grid);
    }

    private void createButtons() {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");
        Button cancelButton = new Button("Zurück");
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.addClickListener(e -> UI.getCurrent().getPage().getHistory().back());
        buttonLayout.add(cancelButton);
        add(buttonLayout);
    }
}

