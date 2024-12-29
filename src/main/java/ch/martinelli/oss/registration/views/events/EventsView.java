package ch.martinelli.oss.registration.views.events;

import ch.martinelli.oss.registration.db.tables.Event;
import ch.martinelli.oss.registration.db.tables.records.EventRecord;
import ch.martinelli.oss.registration.domain.EventRepository;
import ch.martinelli.oss.vaadinjooq.util.VaadinJooqUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.RolesAllowed;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.util.Optional;

@PageTitle("Anlässe")
@Route("events/:eventID?/:action?(edit)")
@Menu(order = 2, icon = LineAwesomeIconUrl.CALENDAR_SOLID)
@RolesAllowed("ADMIN")
public class EventsView extends Div implements BeforeEnterObserver {

    private final String EVENT_ID = "eventID";
    private final String EVENT_EDIT_ROUTE_TEMPLATE = "events/%s/edit";

    private final Grid<EventRecord> grid = new Grid<>(EventRecord.class, false);

    private final Button cancel = new Button("Cancel");
    private final Button save = new Button("Save");

    private final Binder<EventRecord> binder = new Binder<>(EventRecord.class);

    private EventRecord event;

    private final EventRepository eventRepository;

    public EventsView(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
        addClassNames("events-view");

        // Create UI
        SplitLayout splitLayout = new SplitLayout();

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        add(splitLayout);

        // Configure Grid
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        grid.addColumn(EventRecord::getTitle).setHeader("Titel").setAutoWidth(true);
        grid.addColumn(EventRecord::getDescription).setHeader("Beschreibung").setAutoWidth(true);
        grid.addColumn(EventRecord::getLocation).setHeader("Ort").setAutoWidth(true);
        grid.addColumn(EventRecord::getFromDate).setHeader("von").setAutoWidth(true);
        grid.addColumn(EventRecord::getToDate).setHeader("bis").setAutoWidth(true);

        grid.setItems(query -> eventRepository.findAll(
                        query.getOffset(), query.getLimit(),
                        VaadinJooqUtil.orderFields(Event.EVENT, query))
                .stream());

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate(String.format(EVENT_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                clearForm();
                UI.getCurrent().navigate(EventsView.class);
            }
        });

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
        });

        save.addClickListener(e -> {
            try {
                if (this.event == null) {
                    this.event = new EventRecord();
                }
                binder.writeBean(this.event);
                eventRepository.save(this.event);
                clearForm();
                refreshGrid();
                Notification.show("Data updated");
                UI.getCurrent().navigate(EventsView.class);
            } catch (ValidationException validationException) {
                Notification.show("Failed to update the data. Check again that all values are valid");
            }
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> eventId = event.getRouteParameters().get(EVENT_ID).map(Long::parseLong);
        if (eventId.isPresent()) {
            Optional<EventRecord> eventFromBackend = eventRepository.findById(eventId.get());
            if (eventFromBackend.isPresent()) {
                populateForm(eventFromBackend.get());
            } else {
                Notification.show(String.format("The requested event was not found, ID = %s", eventId.get()), 3000,
                        Notification.Position.BOTTOM_START);
                // when a row is selected but the data is no longer available,
                // refresh grid
                refreshGrid();
                event.forwardTo(EventsView.class);
            }
        }
    }

    private void createEditorLayout(SplitLayout splitLayout) {
        Div editorLayoutDiv = new Div();
        editorLayoutDiv.setClassName("editor-layout");

        Div editorDiv = new Div();
        editorDiv.setClassName("editor");
        editorLayoutDiv.add(editorDiv);

        FormLayout formLayout = new FormLayout();

        TextField titleTextField = new TextField("Bezeichnung");
        binder.forField(titleTextField)
                .asRequired()
                .bind(EventRecord::getTitle, EventRecord::setTitle);

        TextArea descriptionTextArea = new TextArea("Bechreibung");
        binder.forField(descriptionTextArea)
                .bind(EventRecord::getDescription, EventRecord::setDescription);

        DatePicker fromDatePicker = new DatePicker("von");
        binder.forField(fromDatePicker)
                .asRequired()
                .bind(EventRecord::getFromDate, EventRecord::setFromDate);

        DatePicker toDatePicker = new DatePicker("bis");
        binder.forField(toDatePicker)
                .bind(EventRecord::getToDate, EventRecord::setToDate);

        formLayout.add(titleTextField, descriptionTextArea, fromDatePicker, toDatePicker);

        editorDiv.add(formLayout);
        createButtonLayout(editorLayoutDiv);

        splitLayout.addToSecondary(editorLayoutDiv);
    }

    private void createButtonLayout(Div editorLayoutDiv) {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonLayout.add(save, cancel);
        editorLayoutDiv.add(buttonLayout);
    }

    private void createGridLayout(SplitLayout splitLayout) {
        Div wrapper = new Div();
        wrapper.setClassName("grid-wrapper");
        splitLayout.addToPrimary(wrapper);
        wrapper.add(grid);
    }

    private void refreshGrid() {
        grid.select(null);
        grid.getDataProvider().refreshAll();
    }

    private void clearForm() {
        populateForm(null);
    }

    private void populateForm(EventRecord value) {
        this.event = value;
        binder.readBean(this.event);

    }
}
