package ch.martinelli.oss.registration.ui.views.events;

import ch.martinelli.oss.registration.db.tables.records.EventRecord;
import ch.martinelli.oss.registration.domain.EventRepository;
import ch.martinelli.oss.registration.ui.components.I18nDatePicker;
import ch.martinelli.oss.registration.ui.components.Notification;
import ch.martinelli.oss.registration.ui.views.EditView;
import ch.martinelli.oss.vaadinjooq.util.VaadinJooqUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static ch.martinelli.oss.registration.db.tables.Event.EVENT;
import static ch.martinelli.oss.registration.ui.components.DateFormat.DATE_FORMAT;
import static com.vaadin.flow.i18n.I18NProvider.translate;

@SuppressWarnings("java:S110")
@Route("events/:eventID?")
@RolesAllowed("ADMIN")
public class EventsView extends EditView<EventRecord> implements BeforeEnterObserver, HasDynamicTitle {

    public static final String EVENT_ID = "eventID";
    private static final String EVENT_ROUTE_TEMPLATE = "events/%s";

    private final transient EventRepository eventRepository;

    public EventsView(EventRepository eventRepository) {
        this.eventRepository = eventRepository;

        grid = new Grid<>(EventRecord.class, false);
        binder = new Binder<>(EventRecord.class);

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
                grid.getDataProvider().refreshAll();
                event.forwardTo(EventsView.class);
            }
        } else {
            populateForm(null);
        }
    }

    @Override
    protected void configureGrid() {
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        grid.addColumn(EventRecord::getTitle)
                .setSortable(true).setSortProperty(EVENT.TITLE.getName())
                .setHeader(translate("title")).setAutoWidth(true);
        grid.addColumn(EventRecord::getDescription)
                .setSortable(true).setSortProperty(EVENT.DESCRIPTION.getName())
                .setHeader(translate("description")).setAutoWidth(true);
        grid.addColumn(EventRecord::getLocation)
                .setSortable(true).setSortProperty(EVENT.LOCATION.getName())
                .setHeader(translate("location")).setAutoWidth(true);
        grid.addColumn(eventRecord -> DATE_FORMAT.format(eventRecord.getFromDate()))
                .setSortable(true).setSortProperty(EVENT.FROM_DATE.getName())
                .setHeader(translate("from")).setAutoWidth(true);
        grid.addColumn(eventRecord -> eventRecord.getToDate() != null ? DATE_FORMAT.format(eventRecord.getToDate()) : "")
                .setSortable(true).setSortProperty(EVENT.TO_DATE.getName())
                .setHeader(translate("until")).setAutoWidth(true);

        Button addButton = new Button(VaadinIcon.PLUS.create());
        addButton.setId("add-event-button");
        addButton.addClickListener(e -> {
            grid.deselectAll();
            grid.getDataProvider().refreshAll();
            EventRecord eventRecord = new EventRecord();
            populateForm(eventRecord);
        });

        grid.addComponentColumn(eventRecord -> {
            Button deleteButton = new Button(VaadinIcon.TRASH.create());
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(e ->
                    new ConfirmDialog(translate("delete.event"),
                            translate("delete.event.question"),
                            translate("yes"),
                            ce -> {
                                try {
                                    eventRepository.delete(eventRecord);

                                    clearForm();
                                    grid.getDataProvider().refreshAll();

                                    Notification.success(translate("delete.event.success"));
                                } catch (DataIntegrityViolationException ex) {
                                    Notification.error(translate("delete.event.error"));
                                }
                            },
                            translate("cancel"),
                            ce -> {
                            }).open());
            return deleteButton;
        }).setHeader(addButton).setTextAlign(ColumnTextAlign.END).setKey("action-column");

        grid.setItems(query -> eventRepository.findAll(
                        query.getOffset(), query.getLimit(),
                        VaadinJooqUtil.orderFields(EVENT, query))
                .stream());

        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate(String.format(EVENT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                clearForm();
                UI.getCurrent().navigate(EventsView.class);
            }
        });
    }

    protected void createComponents(FormLayout formLayout) {
        TextField titleTextField = new TextField(translate("title"));
        binder.forField(titleTextField)
                .asRequired()
                .bind(EventRecord::getTitle, EventRecord::setTitle);
        TextField locationTextField = new TextField(translate("location"));

        binder.forField(locationTextField)
                .asRequired()
                .bind(EventRecord::getLocation, EventRecord::setLocation);
        TextArea descriptionTextArea = new TextArea(translate("description"));

        binder.forField(descriptionTextArea)
                .bind(EventRecord::getDescription, EventRecord::setDescription);
        I18nDatePicker fromDatePicker = new I18nDatePicker(translate("from"));

        binder.forField(fromDatePicker)
                .asRequired()
                .bind(EventRecord::getFromDate, EventRecord::setFromDate);
        I18nDatePicker toDatePicker = new I18nDatePicker(translate("until"));

        binder.forField(toDatePicker)
                .bind(EventRecord::getToDate, EventRecord::setToDate);

        formLayout.add(titleTextField, locationTextField, descriptionTextArea, fromDatePicker, toDatePicker);
    }

    protected void configureButtons() {
        configureCancelButton();

        saveButton.addClickListener(e -> {
            try {
                if (binder.validate().isOk()) {
                    boolean isNew = this.currentRecord.getId() == null;

                    binder.writeBean(this.currentRecord);
                    eventRepository.save(this.currentRecord);

                    if (isNew) {
                        grid.getDataProvider().refreshAll();
                    } else {
                        grid.getDataProvider().refreshItem(this.currentRecord);
                    }

                    Notification.success(translate("save.success"));
                    UI.getCurrent().navigate(EventsView.class);
                }
            } catch (DataIntegrityViolationException | ValidationException dataIntegrityViolationException) {
                Notification.error(translate("save.error"));
            }
        });
    }

    @Override
    public String getPageTitle() {
        return translate("events");
    }
}
