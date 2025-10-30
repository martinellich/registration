package ch.martinelli.oss.registration.ui.views.persons;

import ch.martinelli.oss.registration.domain.ExcelPersonParser;
import ch.martinelli.oss.registration.domain.PersonChangeDetector;
import ch.martinelli.oss.registration.domain.PersonRepository;
import ch.martinelli.oss.registration.ui.components.Notification;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

import java.io.IOException;

import static com.vaadin.flow.i18n.I18NProvider.translate;

/**
 * Dialog for uploading Excel files with person data.
 */
public class PersonUploadDialog extends Dialog {

    private final transient ExcelPersonParser excelPersonParser;

    private final transient PersonChangeDetector personChangeDetector;

    private final transient PersonRepository personRepository;

    private final transient Runnable onSuccess;

    public PersonUploadDialog(ExcelPersonParser excelPersonParser, PersonChangeDetector personChangeDetector,
            PersonRepository personRepository, Runnable onSuccess) {
        this.excelPersonParser = excelPersonParser;
        this.personChangeDetector = personChangeDetector;
        this.personRepository = personRepository;
        this.onSuccess = onSuccess;

        setHeaderTitle(translate("upload.persons.instructions.title"));
        setWidth("500px");

        // Create content
        VerticalLayout content = createContent();
        add(content);

        // Create footer
        var closeButton = new Button(translate("cancel"), e -> close());
        getFooter().add(closeButton);
    }

    private VerticalLayout createContent() {
        var layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        // Instructions
        var instructions = new Paragraph(translate("upload.persons.instructions"));
        instructions.getStyle().set("color", "var(--lumo-secondary-text-color)");

        // Upload component
        var buffer = new MemoryBuffer();
        var upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        upload.setMaxFiles(1);
        upload.setMaxFileSize(10 * 1024 * 1024); // 10 MB
        upload.setDropLabel(new Paragraph(translate("upload.persons.drop.label")));

        // Style the upload area
        var uploadWrapper = new Div(upload);
        uploadWrapper.getStyle()
            .set("border", "2px dashed var(--lumo-contrast-30pct)")
            .set("border-radius", "var(--lumo-border-radius-m)");

        upload.addSucceededListener(event -> {
            try {
                // Parse Excel file
                var excelData = excelPersonParser.parseExcelFile(buffer.getInputStream());

                if (excelData.isEmpty()) {
                    Notification.error(translate("upload.persons.no.data"));
                    return;
                }

                // Detect changes
                var changes = personChangeDetector.detectChanges(excelData);

                if (changes.isEmpty()) {
                    Notification.success(translate("upload.persons.no.changes"));
                    close();
                    return;
                }

                // Open review dialog
                var dialog = new PersonImportDialog(changes, personRepository, () -> {
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                    close();
                });
                dialog.open();

            }
            catch (IOException e) {
                Notification.error(translate("upload.persons.error"));
            }
        });

        upload.addFailedListener(event -> Notification.error(translate("upload.persons.error")));

        layout.add(instructions, uploadWrapper);
        return layout;
    }

}
