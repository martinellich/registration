package ch.martinelli.oss.registration.ui.views;

import ch.martinelli.oss.jooqspring.JooqDAO;
import ch.martinelli.oss.registration.ui.components.Icon;
import ch.martinelli.oss.registration.ui.components.Notification;
import ch.martinelli.oss.vaadinjooq.util.VaadinJooqUtil;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouteParam;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UpdatableRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static com.vaadin.flow.i18n.I18NProvider.translate;

public abstract class EditView<T extends Table<R>, R extends UpdatableRecord<R>, D extends JooqDAO<T, R, Long>> extends Div
        implements BeforeEnterObserver {

    public static final String ID = "id";

    protected final transient D repository;
    private final T table;

    protected Grid<R> grid;
    protected final Button cancelButton = new Button(translate("cancel"));
    protected final Button saveButton = new Button(translate("save"));

    protected Binder<R> binder;
    protected R currentRecord;
    private FormLayout formLayout;
    protected Consumer<R> afterNewRecord;

    public EditView(D repository, T table, Grid<R> grid, Binder<R> binder) {
        this.repository = repository;
        this.table = table;
        this.grid = grid;
        this.binder = binder;

        addClassName("edit-view");

        SplitLayout splitLayout = new SplitLayout();
        splitLayout.addToPrimary(createGridLayout());
        splitLayout.addToSecondary(createEditorLayout());

        add(splitLayout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> personId = event.getRouteParameters().get(ID).map(Long::parseLong);
        if (personId.isPresent()) {
            Optional<R> personFromBackend = repository.findById(personId.get());
            if (personFromBackend.isPresent()) {
                populateForm(personFromBackend.get());
            } else {
                grid.getDataProvider().refreshAll();
                event.forwardTo(this.getClass());
            }
        } else {
            populateForm(null);
        }
    }

    protected Div createGridLayout() {
        Div wrapper = new Div();
        wrapper.setClassName("grid-wrapper");
        wrapper.add(grid);

        configureGrid();
        addActionColumn();
        addSelectionListener();
        setItems();

        return wrapper;
    }

    protected abstract void configureGrid();

    protected void addActionColumn() {
        Icon addIcon = new Icon(LineAwesomeIcon.PLUS_CIRCLE_SOLID, e -> {
            grid.deselectAll();
            grid.getDataProvider().refreshAll();
            R eventRecord = table.newRecord();
            if (afterNewRecord != null) {
                afterNewRecord.accept(eventRecord);
            }
            populateForm(eventRecord);
        });
        addIcon.setId("add-icon");
        addIcon.addClassName("action-icon");

        grid.addComponentColumn(eventRecord -> {
            Icon deleteIcon = new Icon(LineAwesomeIcon.TRASH_SOLID, e ->
                    new ConfirmDialog(translate("delete.record"),
                            translate("delete.record.question"),
                            translate("yes"),
                            ce -> {
                                try {
                                    repository.delete(eventRecord);

                                    clearForm();
                                    grid.getDataProvider().refreshAll();

                                    Notification.success(translate("delete.record.success"));
                                } catch (DataIntegrityViolationException ex) {
                                    Notification.error(translate("delete.record.error"));
                                }
                            },
                            translate("cancel"),
                            ce -> {
                            }).open());
            deleteIcon.addClassName("delete-icon");
            return deleteIcon;
        }).setHeader(addIcon).setTextAlign(ColumnTextAlign.END).setKey("action-column");
    }

    protected void setItems() {
        grid.setItems(query -> repository.findAll(
                        query.getOffset(), query.getLimit(),
                        VaadinJooqUtil.orderFields(table, query))
                .stream());
    }

    protected void addSelectionListener() {
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                TableField<R, ?> idField = Objects.requireNonNull(table.getPrimaryKey()).getFields().getFirst();
                Long id = (Long) event.getValue().get(idField);
                UI.getCurrent().navigate(this.getClass(), new RouteParam(ID, id));
            } else {
                clearForm();
                UI.getCurrent().navigate(this.getClass());
            }
        });
    }

    protected Div createEditorLayout() {
        Div editorLayoutDiv = new Div();
        editorLayoutDiv.setClassName("editor-layout");

        Div editorDiv = new Div();
        editorDiv.setClassName("editor");
        editorLayoutDiv.add(editorDiv);

        formLayout = new FormLayout();
        createComponents(formLayout);

        editorDiv.add(formLayout);
        createButtonLayout(editorLayoutDiv);

        return editorLayoutDiv;
    }

    protected abstract void createComponents(FormLayout formLayout);

    protected void createButtonLayout(Div editorLayoutDiv) {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");

        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonLayout.add(saveButton, cancelButton);

        editorLayoutDiv.add(buttonLayout);

        configureButtons();
    }

    private void configureButtons() {
        saveButton.addClickListener(e -> {
            try {
                if (binder.validate().isOk()) {
                    TableField<R, ?> idField = Objects.requireNonNull(table.getPrimaryKey()).getFields().getFirst();
                    boolean isNew = this.currentRecord.get(idField) == null;

                    binder.writeBean(this.currentRecord);
                    repository.save(this.currentRecord);

                    if (isNew) {
                        grid.getDataProvider().refreshAll();
                    } else {
                        grid.getDataProvider().refreshItem(this.currentRecord);
                    }

                    Notification.success(translate("save.success"));
                    UI.getCurrent().navigate(this.getClass());
                }
            } catch (DataIntegrityViolationException | ValidationException dataIntegrityViolationException) {
                Notification.error(translate("save.error"));
            }
        });

        cancelButton.addClickListener(e -> {
            clearForm();
            grid.getDataProvider().refreshAll();
        });
    }

    protected void clearForm() {
        grid.deselectAll();
        populateForm(null);
    }

    protected void populateForm(R value) {
        this.currentRecord = value;
        binder.readBean(this.currentRecord);

        if (value == null) {
            enableComponents(false);
            cancelButton.setEnabled(false);
            saveButton.setEnabled(false);
        } else {
            enableComponents(true);
            cancelButton.setEnabled(true);
            saveButton.setEnabled(true);
        }
    }

    private void enableComponents(boolean enable) {
        formLayout.getChildren()
                .filter(HasEnabled.class::isInstance)
                .map(HasEnabled.class::cast)
                .forEach(hasEnabled -> hasEnabled.setEnabled(enable));
    }

}
