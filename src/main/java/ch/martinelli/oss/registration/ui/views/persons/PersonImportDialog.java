package ch.martinelli.oss.registration.ui.views.persons;

import ch.martinelli.oss.registration.db.tables.records.PersonRecord;
import ch.martinelli.oss.registration.domain.PersonChange;
import ch.martinelli.oss.registration.domain.PersonRepository;
import ch.martinelli.oss.registration.ui.components.Notification;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.List;

import static ch.martinelli.oss.registration.db.tables.Person.PERSON;
import static com.vaadin.flow.i18n.I18NProvider.translate;

/**
 * Dialog to review and selectively accept/reject person changes from Excel import.
 */
public class PersonImportDialog extends Dialog {

    private final List<PersonChange> changes;

    private final PersonRepository personRepository;

    final Grid<PersonChange> grid; // Package-private for testing

    private final Runnable onSuccess;

    public PersonImportDialog(List<PersonChange> changes, PersonRepository personRepository, Runnable onSuccess) {
        this.changes = changes;
        this.personRepository = personRepository;
        this.onSuccess = onSuccess;

        setHeaderTitle(translate("upload.persons.dialog.title"));
        setWidth("1200px");
        setHeight("800px");

        // Create grid
        this.grid = new Grid<>(PersonChange.class, false);
        configureGrid();

        // Create toolbar
        HorizontalLayout toolbar = createToolbar();

        // Create content layout
        VerticalLayout content = new VerticalLayout(toolbar, grid);
        content.setHeightFull();
        content.setFlexGrow(1, grid);
        content.setPadding(false);
        content.setSpacing(false);
        add(content);

        // Create footer buttons
        createFooter();
    }

    private void configureGrid() {
        grid.setHeightFull();
        // Checkbox column for accept/reject
        grid.addComponentColumn(change -> {
            Checkbox checkbox = new Checkbox();
            checkbox.setValue(change.isAccepted());
            checkbox.addValueChangeListener(e -> change.setAccepted(e.getValue()));
            return checkbox;
        }).setHeader(translate("upload.persons.accept")).setWidth("80px").setFlexGrow(0);

        // Type column (NEW/UPDATE)
        grid.addComponentColumn(change -> {
            Span badge = new Span(translate("upload.persons.type." + change.getType().name().toLowerCase()));
            badge.getElement().getThemeList().add("badge");
            if (change.getType() == PersonChange.ChangeType.NEW) {
                badge.getElement().getThemeList().add("success");
            }
            else {
                badge.getElement().getThemeList().add("contrast");
            }
            return badge;
        }).setHeader(translate("upload.persons.type")).setWidth("100px").setFlexGrow(0);

        // Last Name column
        grid.addColumn(change -> {
            PersonChange.FieldChange fieldChange = change.getChangedFields().get("lastName");
            if (fieldChange != null) {
                return fieldChange.toString();
            }
            return change.getExistingRecord() != null ? change.getExistingRecord().getLastName() : "";
        }).setHeader(translate("last.name")).setAutoWidth(true);

        // First Name column
        grid.addColumn(change -> {
            PersonChange.FieldChange fieldChange = change.getChangedFields().get("firstName");
            if (fieldChange != null) {
                return fieldChange.toString();
            }
            return change.getExistingRecord() != null ? change.getExistingRecord().getFirstName() : "";
        }).setHeader(translate("first.name")).setAutoWidth(true);

        // Email column
        grid.addColumn(change -> {
            PersonChange.FieldChange fieldChange = change.getChangedFields().get("email");
            if (fieldChange != null) {
                return fieldChange.toString();
            }
            return change.getExistingRecord() != null ? change.getExistingRecord().getEmail() : "";
        }).setHeader(translate("email")).setAutoWidth(true);

        // Member ID column
        grid.addColumn(change -> {
            PersonChange.FieldChange fieldChange = change.getChangedFields().get("memberId");
            if (fieldChange != null) {
                return fieldChange.toString();
            }
            return change.getExistingRecord() != null && change.getExistingRecord().getMemberId() != null
                    ? String.valueOf(change.getExistingRecord().getMemberId()) : "";
        }).setHeader(translate("member.id")).setAutoWidth(true);

        grid.setItems(changes);
    }

    private HorizontalLayout createToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        toolbar.setPadding(true);
        toolbar.setSpacing(true);

        Button acceptAllButton = new Button(translate("upload.persons.accept.all"));
        acceptAllButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        acceptAllButton.addClickListener(e -> {
            changes.forEach(change -> change.setAccepted(true));
            grid.getDataProvider().refreshAll();
        });

        Button rejectAllButton = new Button(translate("upload.persons.reject.all"));
        rejectAllButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        rejectAllButton.addClickListener(e -> {
            changes.forEach(change -> change.setAccepted(false));
            grid.getDataProvider().refreshAll();
        });

        toolbar.add(acceptAllButton, rejectAllButton);
        return toolbar;
    }

    private void createFooter() {
        Button cancelButton = new Button(translate("upload.persons.cancel"), e -> close());

        Button applyButton = new Button(translate("upload.persons.apply"), e -> applyChanges());
        applyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        getFooter().add(cancelButton, applyButton);
    }

    private void applyChanges() {
        int appliedCount = 0;

        for (PersonChange change : changes) {
            if (!change.isAccepted()) {
                continue;
            }

            if (change.getType() == PersonChange.ChangeType.NEW) {
                // Create new person
                PersonRecord newRecord = PERSON.newRecord();
                newRecord.setFirstName(change.getNewData().firstName());
                newRecord.setLastName(change.getNewData().lastName());
                newRecord.setEmail(change.getNewData().email());
                newRecord.setMemberId(change.getNewData().memberId());
                newRecord.setActive(true);
                personRepository.save(newRecord);
                appliedCount++;
            }
            else if (change.getType() == PersonChange.ChangeType.UPDATE) {
                // Update existing person
                PersonRecord record = change.getExistingRecord();
                record.setFirstName(change.getNewData().firstName());
                record.setLastName(change.getNewData().lastName());
                record.setEmail(change.getNewData().email());
                record.setMemberId(change.getNewData().memberId());
                personRepository.save(record);
                appliedCount++;
            }
        }

        close();
        Notification.success(translate("upload.persons.success", appliedCount));
        if (onSuccess != null) {
            onSuccess.run();
        }
    }

}
