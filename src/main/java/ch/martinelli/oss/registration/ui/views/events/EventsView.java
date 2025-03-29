package ch.martinelli.oss.registration.ui.views.events;

import ch.martinelli.oss.registration.db.tables.Event;
import ch.martinelli.oss.registration.db.tables.records.EventRecord;
import ch.martinelli.oss.registration.domain.EventRepository;
import ch.martinelli.oss.registration.security.Roles;
import ch.martinelli.oss.registration.ui.components.I18nDatePicker;
import ch.martinelli.oss.registration.ui.views.EditView;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import static ch.martinelli.oss.registration.db.tables.Event.EVENT;
import static ch.martinelli.oss.registration.ui.components.DateFormat.DATE_FORMAT;
import static com.vaadin.flow.i18n.I18NProvider.translate;

@SuppressWarnings("java:S110")
@RolesAllowed({ Roles.USER, Roles.ADMIN })
@Route("events/:" + EditView.ID + "?")
public class EventsView extends EditView<Event, EventRecord, EventRepository>
        implements BeforeEnterObserver, HasDynamicTitle {

    public EventsView(EventRepository eventRepository) {
        super(eventRepository, EVENT, new Grid<>(EventRecord.class, false), new Binder<>(EventRecord.class));
    }

    @Override
    public String getPageTitle() {
        return translate("events");
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

    protected void createComponents(FormLayout formLayout) {
        var titleTextField = new TextField(translate("title"));
        binder.forField(titleTextField).asRequired().bind(EventRecord::getTitle, EventRecord::setTitle);

        var locationTextField = new TextField(translate("location"));
        binder.forField(locationTextField).asRequired().bind(EventRecord::getLocation, EventRecord::setLocation);

        var descriptionTextArea = new TextArea(translate("description"));
        binder.forField(descriptionTextArea).bind(EventRecord::getDescription, EventRecord::setDescription);

        var fromDatePicker = new I18nDatePicker(translate("from"));
        binder.forField(fromDatePicker).asRequired().bind(EventRecord::getFromDate, EventRecord::setFromDate);

        var toDatePicker = new I18nDatePicker(translate("until"));
        binder.forField(toDatePicker).bind(EventRecord::getToDate, EventRecord::setToDate);

        formLayout.add(titleTextField, locationTextField, descriptionTextArea, fromDatePicker, toDatePicker);
    }

}
