package ch.martinelli.oss.registration.views.registration;

import ch.martinelli.oss.registration.db.tables.Event;
import ch.martinelli.oss.registration.db.tables.Registration;
import ch.martinelli.oss.registration.db.tables.records.RegistrationRecord;
import ch.martinelli.oss.registration.domain.RegistrationRepository;
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
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.dao.DataIntegrityViolationException;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.util.Optional;

@PageTitle("Tätigkeitsprogramm")
@Route("registrations/:registrationID?/:action?(edit)")
@Menu(order = 2, icon = LineAwesomeIconUrl.LIST_SOLID)
@RolesAllowed("ADMIN")
public class RegistrationView extends Div implements BeforeEnterObserver {

    private final String REGISTRATION_ID = "registrationID";
    private final String REGISTRATION_EDIT_ROUTE_TEMPLATE = "registrations/%s/edit";

    private final Grid<RegistrationRecord> grid = new Grid<>(RegistrationRecord.class, false);

    private final Button cancel = new Button("Cancel");
    private final Button save = new Button("Save");

    private final Binder<RegistrationRecord> binder = new Binder<>(RegistrationRecord.class);

    private RegistrationRecord registration;

    private final RegistrationRepository registrationRepository;

    public RegistrationView(RegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;

        addClassNames("events-view");

        // Create UI
        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setOrientation(SplitLayout.Orientation.VERTICAL);
        splitLayout.setSplitterPosition(20);

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        add(splitLayout);

        // Configure Grid
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        grid.addColumn(RegistrationRecord::getId)
                .setSortable(true).setSortProperty(Registration.REGISTRATION.ID.getName())
                .setHeader("ID").setAutoWidth(true);
        grid.addColumn(RegistrationRecord::getOpenFrom)
                .setSortable(true).setSortProperty(Registration.REGISTRATION.OPEN_FROM.getName())
                .setHeader("Offen von").setAutoWidth(true);
        grid.addColumn(RegistrationRecord::getOpenFrom)
                .setSortable(true).setSortProperty(Registration.REGISTRATION.OPEN_UNTIL.getName())
                .setHeader("Offen bis").setAutoWidth(true);

        grid.setItems(query -> registrationRepository.findAll(
                        query.getOffset(), query.getLimit(),
                        VaadinJooqUtil.orderFields(Event.EVENT, query))
                .stream());

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate(String.format(REGISTRATION_EDIT_ROUTE_TEMPLATE, event.getValue().getId()));
            } else {
                clearForm();
                UI.getCurrent().navigate(RegistrationView.class);
            }
        });

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
        });

        save.addClickListener(e -> {
            try {
                if (this.registration == null) {
                    this.registration = new RegistrationRecord();
                }
                if (binder.validate().isOk()) {
                    binder.writeBean(this.registration);
                    registrationRepository.save(this.registration);

                    clearForm();
                    refreshGrid();

                    Notification.show("Die Daten wurden gespeichert");
                    UI.getCurrent().navigate(RegistrationView.class);
                }
            } catch (DataIntegrityViolationException | ValidationException dataIntegrityViolationException) {
                Notification.show("Fehler beim Aktualisieren der Daten. Überprüfen Sie, ob alle Werte gültig sind");
            }
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> eventId = event.getRouteParameters().get(REGISTRATION_ID).map(Long::parseLong);
        if (eventId.isPresent()) {
            Optional<RegistrationRecord> registrationFromBackend = registrationRepository.findById(eventId.get());
            if (registrationFromBackend.isPresent()) {
                populateForm(registrationFromBackend.get());
            } else {
                Notification.show(String.format("Die angeforderte Registrierung wurde nicht gefunden, ID = %s", eventId.get()), 3000,
                        Notification.Position.BOTTOM_START);
                // when a row is selected but the data is no longer available, refresh grid
                refreshGrid();
                event.forwardTo(RegistrationView.class);
            }
        }
    }

    private void createEditorLayout(SplitLayout splitLayout) {
        Div editorLayoutDiv = new Div();
        editorLayoutDiv.setWidthFull();
        editorLayoutDiv.setClassName("editor-layout");

        Div editorDiv = new Div();
        editorDiv.setClassName("editor");
        editorLayoutDiv.add(editorDiv);

        FormLayout formLayout = new FormLayout();

        DatePicker openFromDatePicker = new DatePicker("Offen von");
        binder.forField(openFromDatePicker)
                .asRequired()
                .bind(RegistrationRecord::getOpenFrom, RegistrationRecord::setOpenFrom);

        DatePicker openUntilDatePicker = new DatePicker("Offen bis");
        binder.forField(openUntilDatePicker)
                .asRequired()
                .bind(RegistrationRecord::getOpenUntil, RegistrationRecord::setOpenUntil);

        formLayout.add(openFromDatePicker, openUntilDatePicker);

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

    private void populateForm(RegistrationRecord value) {
        this.registration = value;
        binder.readBean(this.registration);

    }
}
