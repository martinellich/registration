package ch.martinelli.oss.registration.ui.views.registration;

import ch.martinelli.oss.registration.domain.EventRegistrationRepository;
import ch.martinelli.oss.registration.domain.EventRegistrationRow;
import ch.martinelli.oss.registration.ui.components.Notification;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;

@PageTitle("Anmeldungen")
@Route("event-registrations")
@RolesAllowed("ADMIN")
@Uses(Icon.class)
public class EventRegistrationView extends Div implements HasUrlParameter<Long> {

    private final transient EventRegistrationRepository eventRegistrationRepository;

    private final Grid<EventRegistrationRow> grid = new Grid<>(EventRegistrationRow.class, false);

    private List<EventRegistrationRow> eventRegistrationMatrix;
    private Long registrationId;

    public EventRegistrationView(EventRegistrationRepository eventRegistrationRepository) {
        this.eventRegistrationRepository = eventRegistrationRepository;

        addClassNames("event-registrations-view");
        setSizeFull();
    }

    @Override
    public void setParameter(BeforeEvent event, Long registrationId) {
        this.registrationId = registrationId;

        removeAll();

        add(createGrid());

        createButtons();
    }

    private Component createGrid() {
        loadData();

        grid.addColumn(EventRegistrationRow::lastName)
                .setHeader("Nachname").setAutoWidth(true);
        grid.addColumn(EventRegistrationRow::firstName)
                .setHeader("Vorname").setAutoWidth(true);

        if (eventRegistrationMatrix.isEmpty()) {
            Notification.warning("Keine Anmeldungen gefunden");
        } else {
            EventRegistrationRow firstRow = eventRegistrationMatrix.getFirst();
            firstRow.registrations().forEach((event, registered) ->
                    grid.addColumn(registrationRow -> registrationRow.registrations().get(event))
                            .setHeader(event).setWidth("20px"));
        }

        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        grid.addClassNames(LumoUtility.Border.TOP, LumoUtility.BorderColor.CONTRAST_10);

        grid.setItems(eventRegistrationMatrix);

        return grid;
    }

    private void loadData() {
        eventRegistrationMatrix = eventRegistrationRepository.getEventRegistrationMatrix(registrationId);
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
