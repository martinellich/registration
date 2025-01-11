package ch.martinelli.oss.registration.ui.views.registration;

import ch.martinelli.oss.registration.db.tables.records.RegistrationRecord;
import ch.martinelli.oss.registration.domain.EventRegistrationRepository;
import ch.martinelli.oss.registration.domain.EventRegistrationRow;
import ch.martinelli.oss.registration.domain.RegistrationRepository;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.RolesAllowed;
import org.jooq.impl.DSL;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.util.List;

import static ch.martinelli.oss.registration.db.tables.Registration.REGISTRATION;
import static com.vaadin.flow.i18n.I18NProvider.translate;

@RolesAllowed({"ADMIN", "USER"})
@Route("event-registrations")
public class EventRegistrationView extends Div implements HasUrlParameter<Long>, HasDynamicTitle {

    private final transient EventRegistrationRepository eventRegistrationRepository;
    private final transient RegistrationRepository registrationRepository;

    private final Select<RegistrationRecord> registrationSelect = new Select<>();
    private Div gridContainer;

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
        registrationSelect.setLabel(translate("invitation"));
        registrationSelect.setItemLabelGenerator(r -> "%s %s".formatted(r.getTitle(), r.getYear().toString()));
        registrationSelect.setItems(registrationRepository.findAll(DSL.noCondition(), List.of(REGISTRATION.YEAR.desc(), REGISTRATION.TITLE)));
        registrationSelect.addValueChangeListener(e -> {
            if (registrationSelect.getValue() != null) {
                this.registrationId = registrationSelect.getValue().getId();
                createGrid();
            }
        });

        Button resetButton = new Button(translate("reset"));
        resetButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        resetButton.addClickListener(e -> {
            registrationSelect.clear();
            createGrid();
        });

        FormLayout formLayout = new FormLayout(registrationSelect, new HorizontalLayout(resetButton));
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        return new VerticalLayout(formLayout);
    }

    private void createGrid() {
        gridContainer.removeAll();

        List<EventRegistrationRow> eventRegistrationMatrix = eventRegistrationRepository.getEventRegistrationMatrix(registrationId);

        Grid<EventRegistrationRow> grid = new Grid<>(EventRegistrationRow.class, false);
        grid.setAllRowsVisible(true);
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.setEmptyStateText(translate("no.registrations"));

        if (!eventRegistrationMatrix.isEmpty()) {

            grid.addColumn(EventRegistrationRow::lastName)
                    .setHeader(translate("last.name"))
                    .setFooter(translate("total"))
                    .setAutoWidth(true);
            grid.addColumn(EventRegistrationRow::firstName)
                    .setHeader(translate("first.name")).setAutoWidth(true);

            EventRegistrationRow firstRow = eventRegistrationMatrix.getFirst();
            firstRow.registrations().forEach((event, r) ->
                    grid.addComponentColumn(registrationRow -> {
                                boolean registered = registrationRow.registrations().get(event);
                                if (registered) {
                                    return LineAwesomeIcon.CHECK_SOLID.create();
                                } else {
                                    return new Span();
                                }
                            })
                            .setHeader(event)
                            .setFooter(calculateNumberOfRegistrations(event))
                            .setWidth("20px"));
        }

        grid.setItems(eventRegistrationMatrix);

        gridContainer.add(new Div(grid));
    }

    private String calculateNumberOfRegistrations(String event) {
        return "" + eventRegistrationRepository.countRegistrationsByEvent(registrationId, event);
    }

    private void createButtons() {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");
        Button cancelButton = new Button(translate("back"));
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.addClickListener(e -> UI.getCurrent().getPage().getHistory().back());
        buttonLayout.add(cancelButton);
        add(buttonLayout);
    }

    @Override
    public String getPageTitle() {
        return translate("event.registrations");
    }
}
