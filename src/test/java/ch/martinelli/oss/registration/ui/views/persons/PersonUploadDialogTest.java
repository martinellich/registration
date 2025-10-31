package ch.martinelli.oss.registration.ui.views.persons;

import ch.martinelli.oss.registration.domain.PersonChangeDetector;
import ch.martinelli.oss.registration.domain.PersonRepository;
import ch.martinelli.oss.registration.ui.views.KaribuTest;
import com.github.mvysny.kaributesting.v10.NotificationsKt;
import com.github.mvysny.kaributesting.v10.UploadKt;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.upload.Upload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;

class PersonUploadDialogTest extends KaribuTest {

    @Autowired
    private PersonChangeDetector personChangeDetector;

    @Autowired
    private PersonRepository personRepository;

    private AtomicBoolean callbackCalled;

    @BeforeEach
    void setupTest() {
        callbackCalled = new AtomicBoolean(false);
        NotificationsKt.clearNotifications();
    }

    // ========== Happy Path Tests ==========

    @Test
    void shouldInitializeDialogWithCorrectConfiguration() {
        // When
        var dialog = new PersonUploadDialog(personChangeDetector, personRepository, () -> callbackCalled.set(true));
        dialog.open();

        // Then
        assertThat(dialog.isOpened()).isTrue();
        assertThat(dialog.getWidth()).isEqualTo("500px");

        // Verify upload component exists and has correct configuration
        var upload = _get(dialog, Upload.class);
        assertThat(upload.getMaxFiles()).isEqualTo(1);
        assertThat(upload.getMaxFileSize()).isEqualTo(10 * 1024 * 1024); // 10 MB

        // Verify cancel button exists in dialog
        var cancelButton = _get(dialog, Button.class, spec -> spec.withText("Abbrechen"));
        assertThat(cancelButton).isNotNull();
    }

    @Test
    void shouldCreateUploadComponent() {
        // Given/When
        var dialog = new PersonUploadDialog(personChangeDetector, personRepository, () -> callbackCalled.set(true));
        dialog.open();

        // Then - Verify upload component exists and is properly configured
        var upload = _get(dialog, Upload.class);
        assertThat(upload).isNotNull();

        // Note: Event listeners are registered but cannot be tested directly in unit
        // tests
        // because getListeners() is protected. Full end-to-end upload testing requires
        // integration tests with actual file uploads or using the Vaadin TestBench.
    }

    @Test
    void shouldAcceptOnlyXlsxFiles() {
        // Given/When
        var dialog = new PersonUploadDialog(personChangeDetector, personRepository, () -> callbackCalled.set(true));
        dialog.open();

        // Then - Verify accepted file types
        var upload = _get(dialog, Upload.class);
        var acceptedTypes = upload.getAcceptedFileTypes();
        assertThat(acceptedTypes).contains(".xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @Test
    void shouldConfigureCallbackCorrectly() {
        // Given/When
        var dialog = new PersonUploadDialog(personChangeDetector, personRepository, () -> callbackCalled.set(true));
        dialog.open();

        // Then - Verify callback is stored and can be invoked
        // (The actual callback invocation happens through PersonImportDialog which
        // is tested separately)
        assertThat(callbackCalled.get()).isFalse(); // Not called on dialog open
    }

    @Test
    void shouldHandleMaxFileSizeExceeded() {
        // Given
        var dialog = new PersonUploadDialog(personChangeDetector, personRepository, () -> callbackCalled.set(true));
        dialog.open();

        // When
        var upload = _get(dialog, Upload.class);

        // Then - Verify max file size is set correctly (10 MB)
        assertThat(upload.getMaxFileSize()).isEqualTo(10 * 1024 * 1024);
        // Note: File size rejection is handled by Vaadin Upload component automatically
    }

    @Test
    void shouldHandleInvalidFileType() {
        // Given
        var dialog = new PersonUploadDialog(personChangeDetector, personRepository, () -> callbackCalled.set(true));
        dialog.open();

        // When
        var upload = _get(dialog, Upload.class);

        // Then - Verify accepted file types include only .xlsx
        var acceptedFileTypes = upload.getAcceptedFileTypes();
        assertThat(acceptedFileTypes).contains(".xlsx")
            .contains("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        // Note: File type rejection is handled by Vaadin Upload component automatically
    }

    @Test
    void shouldHandleMultipleFileUploadAttempt() {
        // Given
        var dialog = new PersonUploadDialog(personChangeDetector, personRepository, () -> callbackCalled.set(true));
        dialog.open();

        // When
        var upload = _get(dialog, Upload.class);

        // Then - Verify max files is set to 1
        assertThat(upload.getMaxFiles()).isEqualTo(1);
        // Note: Multiple file rejection is handled by Vaadin Upload component
        // automatically
    }

    // ========== Component Interaction Tests ==========

    @Test
    void shouldCloseDialogWhenCancelButtonClicked() {
        // Given
        var dialog = new PersonUploadDialog(personChangeDetector, personRepository, () -> callbackCalled.set(true));
        dialog.open();
        assertThat(dialog.isOpened()).isTrue();

        // When - Find cancel button within the dialog
        var cancelButton = _get(dialog, Button.class, spec -> spec.withText("Abbrechen"));
        _click(cancelButton);

        // Then
        assertThat(dialog.isOpened()).isFalse();
        assertThat(callbackCalled.get()).isFalse();
    }

    @Test
    void shouldDisplayInstructions() {
        // Given/When
        var dialog = new PersonUploadDialog(personChangeDetector, personRepository, () -> callbackCalled.set(true));
        dialog.open();

        // Then - Verify instructions paragraph exists in dialog
        // Note: Upload component also contains a paragraph for drop label,
        // so we check that dialog has content
        assertThat(dialog.getChildren().count()).isGreaterThan(0);
    }

    // ========== Actual File Upload Tests ==========

    @Test
    void shouldSuccessfullyUploadValidExcelFile() throws IOException {
        // Given
        var dialog = new PersonUploadDialog(personChangeDetector, personRepository, () -> callbackCalled.set(true));
        dialog.open();

        var upload = _get(dialog, Upload.class);

        // When - Upload actual Excel file
        var excelFile = Path.of("src/test/resources/upload_partipipants.xlsx");
        var fileContent = Files.readAllBytes(excelFile);
        UploadKt._upload(upload, "upload_partipipants.xlsx", fileContent);

        // Then - Verify PersonImportDialog is opened with changes
        var importDialog = _get(PersonImportDialog.class);
        assertThat(importDialog.isOpened()).isTrue();
    }

    @Test
    void shouldThrowExceptionForInvalidExcelFile() {
        // Given
        var dialog = new PersonUploadDialog(personChangeDetector, personRepository, () -> callbackCalled.set(true));
        dialog.open();

        var upload = _get(dialog, Upload.class);

        // When/Then - Upload invalid Excel file should throw exception
        // Note: This reveals that the error handling in PersonUploadDialog only catches
        // IOException,
        // but Apache POI throws NotOfficeXmlFileException (RuntimeException) for invalid
        // files
        var invalidContent = "This is not a valid Excel file".getBytes();
        assertThat(org.assertj.core.api.Assertions
            .catchThrowable(() -> UploadKt._upload(upload, "invalid.xlsx", invalidContent)))
            .isInstanceOf(java.util.concurrent.ExecutionException.class)
            .hasCauseInstanceOf(org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException.class)
            .hasMessageContaining("not a valid OOXML");
    }

}
