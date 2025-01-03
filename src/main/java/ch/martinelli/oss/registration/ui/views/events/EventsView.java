package ch.martinelli.oss.registration.ui.views.events;

import ch.martinelli.oss.registration.db.tables.records.EventRecord;
import ch.martinelli.oss.registration.domain.EventRepository;
import ch.martinelli.oss.registration.ui.components.I18nDatePicker;
import ch.martinelli.oss.registration.ui.components.Notification;
import ch.martinelli.oss.vaadinjooq.util.VaadinJooqUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.dao.DataIntegrityViolationException;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.util.Optional;

import static ch.martinelli.oss.registration.db.tables.Event.EVENT;

@PageTitle("Anlässe")
@Route("events/:eventID?/:action?(edit)")
@Menu(order = 3, icon = LineAwesomeIconUrl.CALENDAR_SOLID)
@RolesAllowed("ADMIN")
public class EventsView extends Div implements BeforeEnterObserver {

    public static final String EVENT_ID = "eventID";
    private static final String EVENT_EDIT_ROUTE_TEMPLATE = "events/%s/edit";

    private final transient EventRepository eventRepository;

    private final Grid<EventRecord> grid = new Grid<>(EventRecord.class, false);
    private final Button cancel = new Button("Abbrechen");
    private final Button save = new Button("Speichern");

    private final Binder<EventRecord> binder = new Binder<>(EventRecord.class);
    private EventRecord event;

    public EventsView(EventRepository eventRepository) {
        this.eventRepository = eventRepository;

        addClassNames("events-view");

        SplitLayout splitLayout = new SplitLayout();
        splitLayout.addToPrimary(createGridLayout());
        splitLayout.addToSecondary(createEditorLayout());

        add(splitLayout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> eventId = event.getRouteParameters().get(EVENT_ID).map(Long::parseLong);
        if (eventId.isPresent()) {
            Optional<EventRecord> eventFromBackend = eventRepository.findById(eventId.get());
            if (eventFromBackend.isPresent()) {
                populateForm(eventFromBackend.get());
            } else {
                // when a row is selected but the data is no longer available, refresh grid
                grid.getDataProvider().refreshAll();
                event.forwardTo(EventsView.class);
            }
        }
    }

    private Div createGridLayout() {
        Div wrapper = new Div();
        wrapper.setClassName("grid-wrapper");
        wrapper.add(grid);

        configureGrid();

        return wrapper;
    }

    private void configureGrid() {
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        grid.addColumn(EventRecord::getTitle)
                .setSortable(true).setSortProperty(EVENT.TITLE.getName())
                .setHeader("Titel").setAutoWidth(true);
        grid.addColumn(EventRecord::getDescription)
                .setSortable(true).setSortProperty(EVENT.DESCRIPTION.getName())
                .setHeader("Beschreibung").setAutoWidth(true);
        grid.addColumn(EventRecord::getLocation)
                .setSortable(true).setSortProperty(EVENT.LOCATION.getName())
                .setHeader("Ort").setAutoWidth(true);
        grid.addColumn(EventRecord::getFromDate)
                .setSortable(true).setSortProperty(EVENT.FROM_DATE.getName())
                .setHeader("von").setAutoWidth(true);
        grid.addColumn(EventRecord::getToDate)
                .setSortable(true).setSortProperty(EVENT.TO_DATE.getName())
                .setHeader("bis").setAutoWidth(true);

        Button addButton = new Button(VaadinIcon.PLUS.create());
        addButton.setId("add-event-button");
        addButton.addClickListener(e -> {
            clearForm();
            grid.getDataProvider().refreshAll();
        });

        grid.addComponentColumn(eventRecord -> {
            Button deleteButton = new Button(VaadinIcon.TRASH.create());
            deleteButton.addClickListener(e ->
                    new ConfirmDialog("Anlass löschen",
                            "Willst du den Anlass wirklich löschen?",
                            "Ja",
                            ce -> {
                                try {
                                    eventRepository.delete(eventRecord);

                                    clearForm();
                                    grid.getDataProvider().refreshAll();

                                    Notification.success("Der Anlass wurde gelöscht");
                                } catch (DataIntegrityViolationException ex) {
                                    Notification.error("Der Anlass wird noch verwendet und kann nicht gelöscht werden");
                                }
                            },
                            "Abbrechen",
                            ce -> {
                            }).open());
            return deleteButton;
        }).setHeader(addButton).setTextAlign(ColumnTextAlign.END).setKey("action-column");

        grid.setItems(query -> eventRepository.findAll(
                        query.getOffset(), query.getLimit(),
                        VaadinJooqUtil.orderFields(EVENT, query))
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
    }

    private Div createEditorLayout() {
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

        TextField locationTextField = new TextField("Ort");
        binder.forField(locationTextField)
                .asRequired()
                .bind(EventRecord::getLocation, EventRecord::setLocation);

        TextArea descriptionTextArea = new TextArea("Beschreibung");
        binder.forField(descriptionTextArea)
                .bind(EventRecord::getDescription, EventRecord::setDescription);

        I18nDatePicker fromDatePicker = new I18nDatePicker("von");
        binder.forField(fromDatePicker)
                .asRequired()
                .bind(EventRecord::getFromDate, EventRecord::setFromDate);

        I18nDatePicker toDatePicker = new I18nDatePicker("bis");
        binder.forField(toDatePicker)
                .bind(EventRecord::getToDate, EventRecord::setToDate);

        formLayout.add(titleTextField, locationTextField, descriptionTextArea, fromDatePicker, toDatePicker);

        editorDiv.add(formLayout);
        createButtonLayout(editorLayoutDiv);

        return editorLayoutDiv;
    }

    private void createButtonLayout(Div editorLayoutDiv) {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");

        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonLayout.add(save, cancel);

        editorLayoutDiv.add(buttonLayout);

        configureButtons();
    }

    private void configureButtons() {
        cancel.addClickListener(e -> {
            clearForm();
            grid.getDataProvider().refreshAll();
        });

        save.addClickListener(e -> {
            try {
                if (this.event == null) {
                    this.event = new EventRecord();
                }
                if (binder.validate().isOk()) {
                    binder.writeBean(this.event);
                    eventRepository.save(this.event);

                    grid.getDataProvider().refreshItem(this.event);

                    Notification.success("Die Daten wurden gespeichert");
                    UI.getCurrent().navigate(EventsView.class);
                }
            } catch (DataIntegrityViolationException | ValidationException dataIntegrityViolationException) {
                Notification.error("Fehler beim Aktualisieren der Daten. Überprüfen Sie, ob alle Werte gültig sind");
            }
        });
    }

    private void clearForm() {
        grid.deselectAll();
        populateForm(null);
    }

    private void populateForm(EventRecord value) {
        this.event = value;
        binder.readBean(this.event);
    }
}
