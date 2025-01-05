package ch.martinelli.oss.registration.ui.views;

import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.binder.Binder;
import org.jooq.Record;

import static com.vaadin.flow.i18n.I18NProvider.translate;

public abstract class EditView<R extends Record> extends Div {

    protected Grid<R> grid;
    protected final Button cancelButton = new Button(translate("cancel"));
    protected final Button saveButton = new Button(translate("save"));

    protected Binder<R> binder;
    protected R currentRecord;
    private FormLayout formLayout;

    protected EditView() {
        addClassName("edit-view");
    }

    protected Div createGridLayout() {
        Div wrapper = new Div();
        wrapper.setClassName("grid-wrapper");
        wrapper.add(grid);

        configureGrid();

        return wrapper;
    }

    protected abstract void configureGrid();

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

    protected abstract void configureButtons();

    protected void configureCancelButton() {
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
