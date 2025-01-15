package ch.martinelli.oss.registration.domain;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
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
        List<EventRegistrationRow> rows = eventRegistrationRepository.getEventRegistrationMatrix(registrationId);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(translate("event.registrations"));

            // First, collect all possible registration types (Map keys).
            // Using TreeSet for sorted columns
            Set<String> allRegistrationTypes = new TreeSet<>();

            for (EventRegistrationRow row : rows) {
                allRegistrationTypes.addAll(row.registrations().keySet());
            }

            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue(translate("last.name"));
            headerRow.createCell(1).setCellValue(translate("first.name"));
            int columnIndex = 2;
            for (String registrationType : allRegistrationTypes) {
                headerRow.createCell(columnIndex++).setCellValue(registrationType);
            }

            int rowIndex = 1;
            for (EventRegistrationRow row : rows) {
                Row dataRow = sheet.createRow(rowIndex++);
                dataRow.createCell(0).setCellValue(row.lastName());
                dataRow.createCell(1).setCellValue(row.firstName());

                columnIndex = 2;
                for (String registrationType : allRegistrationTypes) {
                    Cell cell = dataRow.createCell(columnIndex++);
                    Boolean value = row.registrations().get(registrationType);
                    if (Boolean.TRUE.equals(value)) {
                        cell.setCellValue("X");
                    }
                    // false values remain empty
                }
            }

            for (int i = 0; i < allRegistrationTypes.size() + 2; i++) {
                sheet.autoSizeColumn(i);
            }

            try (ByteArrayOutputStream fileOut = new ByteArrayOutputStream()) {
                workbook.write(fileOut);
                return fileOut.toByteArray();
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
