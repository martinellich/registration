package ch.martinelli.oss.registration.ui.views.persons;

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

    private final transient List<PersonChange> changes;

    private final transient PersonRepository personRepository;

    final Grid<PersonChange> grid; // Package-private for testing

    private final transient Runnable onSuccess;

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
        var toolbar = createToolbar();

        // Create content layout
        var content = new VerticalLayout(toolbar, grid);
        content.setHeightFull();
        content.setFlexGrow(1, grid);
        content.setPadding(false);
        content.setSpacing(false);
        add(content);

        // Create footer buttons
        createFooter();
    }

    @SuppressWarnings("java:S3776")
    private void configureGrid() {
        grid.setHeightFull();
        // Checkbox column for accept/reject
        grid.addComponentColumn(change -> {
            Checkbox checkbox = new Checkbox();
            checkbox.setValue(change.isAccepted());
            checkbox.addValueChangeListener(e -> change.setAccepted(e.getValue()));
            return checkbox;
        }).setHeader(translate("upload.persons.accept")).setWidth("80px").setFlexGrow(0);

        // Type column (NEW/UPDATE/DEACTIVATE)
        grid.addComponentColumn(change -> {
            Span badge = new Span(translate("upload.persons.type." + change.getType().name().toLowerCase()));
            badge.getElement().getThemeList().add("badge");
            if (change.getType() == PersonChange.ChangeType.NEW) {
                badge.getElement().getThemeList().add("success");
            }
            else if (change.getType() == PersonChange.ChangeType.DEACTIVATE) {
                badge.getElement().getThemeList().add("error");
            }
            else {
                badge.getElement().getThemeList().add("contrast");
            }
            return badge;
        }).setHeader(translate("upload.persons.type")).setWidth("120px").setFlexGrow(0);

        // Last Name column
        grid.addColumn(change -> {
            PersonChange.FieldChange fieldChange = change.getChangedFields().get("lastName");
            if (fieldChange != null) {
                return fieldChange.toString();
            }
            if (change.getExistingRecord() != null) {
                return change.getExistingRecord().getLastName();
            }
            return change.getNewData() != null ? change.getNewData().lastName() : "";
        }).setHeader(translate("last.name")).setAutoWidth(true);

        // First Name column
        grid.addColumn(change -> {
            PersonChange.FieldChange fieldChange = change.getChangedFields().get("firstName");
            if (fieldChange != null) {
                return fieldChange.toString();
            }
            if (change.getExistingRecord() != null) {
                return change.getExistingRecord().getFirstName();
            }
            return change.getNewData() != null ? change.getNewData().firstName() : "";
        }).setHeader(translate("first.name")).setAutoWidth(true);

        // Email column
        grid.addColumn(change -> {
            PersonChange.FieldChange fieldChange = change.getChangedFields().get("email");
            if (fieldChange != null) {
                return fieldChange.toString();
            }
            if (change.getExistingRecord() != null) {
                return change.getExistingRecord().getEmail();
            }
            return change.getNewData() != null ? change.getNewData().email() : "";
        }).setHeader(translate("email")).setAutoWidth(true);

        // Member ID column
        grid.addColumn(change -> {
            PersonChange.FieldChange fieldChange = change.getChangedFields().get("memberId");
            if (fieldChange != null) {
                return fieldChange.toString();
            }
            if (change.getExistingRecord() != null && change.getExistingRecord().getMemberId() != null) {
                return String.valueOf(change.getExistingRecord().getMemberId());
            }
            return change.getNewData() != null && change.getNewData().memberId() != null
                    ? String.valueOf(change.getNewData().memberId()) : "";
        }).setHeader(translate("member.id")).setAutoWidth(true);

        grid.setItems(changes);
    }

    private HorizontalLayout createToolbar() {
        var toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        toolbar.setPadding(true);
        toolbar.setSpacing(true);

        var acceptAllButton = new Button(translate("upload.persons.accept.all"));
        acceptAllButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        acceptAllButton.addClickListener(e -> {
            changes.forEach(change -> change.setAccepted(true));
            grid.getDataProvider().refreshAll();
        });

        var rejectAllButton = new Button(translate("upload.persons.reject.all"));
        rejectAllButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        rejectAllButton.addClickListener(e -> {
            changes.forEach(change -> change.setAccepted(false));
            grid.getDataProvider().refreshAll();
        });

        toolbar.add(acceptAllButton, rejectAllButton);
        return toolbar;
    }

    private void createFooter() {
        var cancelButton = new Button(translate("upload.persons.cancel"), e -> close());

        var applyButton = new Button(translate("upload.persons.apply"), e -> applyChanges());
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
                var newRecord = PERSON.newRecord();
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
                var personRecord = change.getExistingRecord();
                personRecord.setFirstName(change.getNewData().firstName());
                personRecord.setLastName(change.getNewData().lastName());
                personRecord.setEmail(change.getNewData().email());
                personRecord.setMemberId(change.getNewData().memberId());
                personRepository.save(personRecord);
                appliedCount++;
            }
            else if (change.getType() == PersonChange.ChangeType.DEACTIVATE) {
                // Deactivate person not in Excel
                var personRecord = change.getExistingRecord();
                personRecord.setActive(false);
                personRepository.save(personRecord);
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
