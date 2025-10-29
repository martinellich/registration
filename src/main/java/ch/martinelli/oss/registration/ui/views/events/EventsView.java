package ch.martinelli.oss.registration.ui.views.events;

import ch.martinelli.oss.registration.db.tables.Event;
import ch.martinelli.oss.registration.db.tables.records.EventRecord;
import ch.martinelli.oss.registration.domain.EventRepository;
import ch.martinelli.oss.registration.security.Roles;
import ch.martinelli.oss.registration.ui.components.I18nDatePicker;
import ch.martinelli.oss.registration.ui.views.EditView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.LocalDate;

import static ch.martinelli.oss.registration.db.tables.Event.EVENT;
import static ch.martinelli.oss.registration.ui.components.DateFormat.DATE_FORMAT;
import static com.vaadin.flow.i18n.I18NProvider.translate;

@SuppressWarnings("java:S110")
@RolesAllowed({ Roles.USER, Roles.ADMIN })
@Route("events/:" + EditView.ID + "?")
public class EventsView extends EditView<Event, EventRecord, EventRepository>
        implements BeforeEnterObserver, HasDynamicTitle {

    private boolean hidePastEvents;

    public EventsView(EventRepository eventRepository) {
        super(eventRepository, EVENT, new Grid<>(EventRecord.class, false), new Binder<>(EventRecord.class));

        this.hidePastEvents = true; // Initialize in constructor
    }

    @Override
    public String getPageTitle() {
        return translate("events");
    }

    @Override
    protected Div createGridLayout() {
        var wrapper = new Div();
        wrapper.setClassName("grid-wrapper");

        // Add toolbar with filter button
        var toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);
        toolbar.setPadding(true);
        toolbar.setSpacing(true);

        // When hidePastEvents is true (default), button should show "Show past events"
        // action
        var togglePastEventsButton = new Button(translate("show.past.events"));
        togglePastEventsButton.setId("toggle-past-events-button");
        togglePastEventsButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        togglePastEventsButton.addClickListener(event -> {
            hidePastEvents = !hidePastEvents;
            togglePastEventsButton
                .setText(hidePastEvents ? translate("show.past.events") : translate("hide.past.events"));
            grid.getDataProvider().refreshAll();
        });

        toolbar.add(togglePastEventsButton);

        wrapper.add(toolbar, grid);

        configureGrid();
        addActionColumn();
        addSelectionListener();
        setItems();

        return wrapper;
    }

    @Override
    protected void configureGrid() {
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        var titleColumn = grid.addColumn(EventRecord::getTitle)
            .setSortable(true)
            .setSortProperty(EVENT.TITLE.getName())
            .setHeader(translate("title"))
            .setAutoWidth(true);
        grid.addColumn(EventRecord::getDescription)
            .setSortable(true)
            .setSortProperty(EVENT.DESCRIPTION.getName())
            .setHeader(translate("description"))
            .setAutoWidth(true);
        grid.addColumn(EventRecord::getLocation)
            .setSortable(true)
            .setSortProperty(EVENT.LOCATION.getName())
            .setHeader(translate("location"))
            .setAutoWidth(true);
        grid.addColumn(
                eventRecord -> Boolean.TRUE.equals(eventRecord.getMandatory()) ? translate("yes") : translate("no"))
            .setSortable(true)
            .setSortProperty(EVENT.MANDATORY.getName())
            .setHeader(translate("mandatory"))
            .setAutoWidth(true);
        grid.addColumn(eventRecord -> DATE_FORMAT.format(eventRecord.getFromDate()))
            .setSortable(true)
            .setSortProperty(EVENT.FROM_DATE.getName())
            .setHeader(translate("from"))
            .setAutoWidth(true);
        grid.addColumn(
                eventRecord -> eventRecord.getToDate() != null ? DATE_FORMAT.format(eventRecord.getToDate()) : "")
            .setSortable(true)
            .setSortProperty(EVENT.TO_DATE.getName())
            .setHeader(translate("until"))
            .setAutoWidth(true);

        grid.sort(GridSortOrder.asc(titleColumn).build());
    }

    @Override
    protected void setItems() {
        grid.setItems(query -> repository
            .findAll(query.getOffset(), query.getLimit(),
                    ch.martinelli.oss.vaadinjooq.util.VaadinJooqUtil.orderFields(EVENT, query))
            .stream()
            .filter(event -> {
                if (!hidePastEvents) {
                    return true; // Show all events
                }
                // Use toDate if present, otherwise use fromDate
                var eventEndDate = event.getToDate() != null ? event.getToDate() : event.getFromDate();
                return eventEndDate.isAfter(LocalDate.now()) || eventEndDate.isEqual(LocalDate.now());
            }));
    }

    protected void createComponents(FormLayout formLayout) {
        var titleTextField = new TextField(translate("title"));
        binder.forField(titleTextField).asRequired().bind(EventRecord::getTitle, EventRecord::setTitle);

        var locationTextField = new TextField(translate("location"));
        binder.forField(locationTextField).asRequired().bind(EventRecord::getLocation, EventRecord::setLocation);

        var descriptionTextArea = new TextArea(translate("description"));
        binder.forField(descriptionTextArea).bind(EventRecord::getDescription, EventRecord::setDescription);

        var mandatoryCheckbox = new Checkbox(translate("mandatory"));
        binder.forField(mandatoryCheckbox).bind(EventRecord::getMandatory, EventRecord::setMandatory);

        var fromDatePicker = new I18nDatePicker(translate("from"));
        binder.forField(fromDatePicker).asRequired().bind(EventRecord::getFromDate, EventRecord::setFromDate);

        var toDatePicker = new I18nDatePicker(translate("until"));
        binder.forField(toDatePicker).bind(EventRecord::getToDate, EventRecord::setToDate);

        formLayout.add(titleTextField, locationTextField, descriptionTextArea, mandatoryCheckbox, fromDatePicker,
                toDatePicker);
    }

}
