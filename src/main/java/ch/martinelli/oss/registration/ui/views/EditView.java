package ch.martinelli.oss.registration.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.binder.Binder;
import org.jooq.Record;

public abstract class EditView<R extends Record> extends Div {

    protected Grid<R> grid;
    protected final Button cancel = new Button("Abbrechen");
    protected final Button save = new Button("Speichern");

    protected Binder<R> binder;
    protected R currentRecord;

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

        FormLayout formLayout = new FormLayout();
        createComponents(formLayout);

        editorDiv.add(formLayout);
        createButtonLayout(editorLayoutDiv);

        return editorLayoutDiv;
    }

    protected abstract void createComponents(FormLayout formLayout);

    protected void createButtonLayout(Div editorLayoutDiv) {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");

        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonLayout.add(save, cancel);

        editorLayoutDiv.add(buttonLayout);

        configureButtons();
    }

    protected abstract void configureButtons();

    protected void clearForm() {
        grid.deselectAll();
        populateForm(null);
    }

    protected void populateForm(R value) {
        this.currentRecord = value;
        binder.readBean(this.currentRecord);
    }

}
