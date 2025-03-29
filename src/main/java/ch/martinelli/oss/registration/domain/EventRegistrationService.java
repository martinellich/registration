package ch.martinelli.oss.registration.domain;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.TreeSet;

import static com.vaadin.flow.i18n.I18NProvider.translate;

@Service
public class EventRegistrationService {

    private final EventRegistrationRepository eventRegistrationRepository;

    public EventRegistrationService(EventRegistrationRepository eventRegistrationRepository) {
        this.eventRegistrationRepository = eventRegistrationRepository;
    }

    @SuppressWarnings("java:S112")
    public byte[] createEventRegistrationExcel(Long registrationId) {
        var eventRegistrationRows = eventRegistrationRepository.getEventRegistrationMatrix(registrationId);

        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet(translate("event.registrations"));

            // First, collect all possible registration types (Map keys).
            // Using TreeSet for sorted columns
            var allRegistrationTypes = new TreeSet<String>();

            for (var eventRegistrationRow : eventRegistrationRows) {
                allRegistrationTypes.addAll(eventRegistrationRow.registrations().keySet());
            }

            var headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue(translate("last.name"));
            headerRow.createCell(1).setCellValue(translate("first.name"));
            var columnIndex = 2;
            for (var registrationType : allRegistrationTypes) {
                headerRow.createCell(columnIndex++).setCellValue(registrationType);
            }

            var rowIndex = 1;
            for (var eventRegistrationRow : eventRegistrationRows) {
                var dataRow = sheet.createRow(rowIndex++);
                dataRow.createCell(0).setCellValue(eventRegistrationRow.lastName());
                dataRow.createCell(1).setCellValue(eventRegistrationRow.firstName());

                columnIndex = 2;
                for (var registrationType : allRegistrationTypes) {
                    var cell = dataRow.createCell(columnIndex++);
                    var value = eventRegistrationRow.registrations().get(registrationType);
                    if (Boolean.TRUE.equals(value)) {
                        cell.setCellValue("X");
                    }
                    // false values remain empty
                }
            }

            for (var i = 0; i < allRegistrationTypes.size() + 2; i++) {
                sheet.autoSizeColumn(i);
            }

            try (var fileOut = new ByteArrayOutputStream()) {
                workbook.write(fileOut);
                return fileOut.toByteArray();
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
