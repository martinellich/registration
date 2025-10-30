package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.registration.db.tables.records.PersonRecord;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to detect changes between Excel data and existing database records. Matches
 * Excel rows to database records and identifies new persons and updates.
 */
@Service
public class PersonChangeDetector {

    private final PersonRepository personRepository;

    public PersonChangeDetector(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    /**
     * Detect changes between Excel data and database records.
     * @param excelData List of persons parsed from Excel
     * @return List of detected changes (NEW or UPDATE only, NO_CHANGE filtered out)
     */
    public List<PersonChange> detectChanges(List<ExcelPersonData> excelData) {
        // Load all existing persons from database
        List<PersonRecord> allPersons = personRepository.findAll(DSL.trueCondition());

        List<PersonChange> changes = new ArrayList<>();

        for (ExcelPersonData data : excelData) {
            PersonRecord existingRecord = findMatchingRecord(data, allPersons);

            if (existingRecord == null) {
                // No match found - this is a new person
                changes.add(createNewPersonChange(data));
            }
            else {
                // Match found - check if there are any changes
                PersonChange change = createUpdateChange(existingRecord, data);
                if (change.getType() == PersonChange.ChangeType.UPDATE) {
                    changes.add(change);
                }
                // Skip NO_CHANGE
            }
        }

        return changes;
    }

    /**
     * Find matching record using member_id first, then firstName + lastName.
     */
    private PersonRecord findMatchingRecord(ExcelPersonData data, List<PersonRecord> allPersons) {
        // First try to match by member_id if available
        if (data.memberId() != null) {
            Optional<PersonRecord> byMemberId = allPersons.stream()
                .filter(p -> Objects.equals(p.getMemberId(), data.memberId()))
                .findFirst();
            if (byMemberId.isPresent()) {
                return byMemberId.get();
            }
        }

        // Fall back to matching by firstName + lastName
        return allPersons.stream()
            .filter(p -> Objects.equals(p.getFirstName(), data.firstName())
                    && Objects.equals(p.getLastName(), data.lastName()))
            .findFirst()
            .orElse(null);
    }

    private PersonChange createNewPersonChange(ExcelPersonData data) {
        Map<String, PersonChange.FieldChange> changedFields = new HashMap<>();
        changedFields.put("firstName", new PersonChange.FieldChange(null, data.firstName()));
        changedFields.put("lastName", new PersonChange.FieldChange(null, data.lastName()));
        if (data.email() != null && !data.email().isBlank()) {
            changedFields.put("email", new PersonChange.FieldChange(null, data.email()));
        }
        if (data.memberId() != null) {
            changedFields.put("memberId", new PersonChange.FieldChange(null, String.valueOf(data.memberId())));
        }

        return new PersonChange(PersonChange.ChangeType.NEW, null, data, changedFields);
    }

    private PersonChange createUpdateChange(PersonRecord existingRecord, ExcelPersonData data) {
        Map<String, PersonChange.FieldChange> changedFields = new HashMap<>();

        // Compare firstName
        if (!Objects.equals(existingRecord.getFirstName(), data.firstName())) {
            changedFields.put("firstName",
                    new PersonChange.FieldChange(existingRecord.getFirstName(), data.firstName()));
        }

        // Compare lastName
        if (!Objects.equals(existingRecord.getLastName(), data.lastName())) {
            changedFields.put("lastName", new PersonChange.FieldChange(existingRecord.getLastName(), data.lastName()));
        }

        // Compare email
        if (!Objects.equals(existingRecord.getEmail(), data.email())) {
            changedFields.put("email", new PersonChange.FieldChange(existingRecord.getEmail(), data.email()));
        }

        // Compare memberId
        if (!Objects.equals(existingRecord.getMemberId(), data.memberId())) {
            String oldValue = existingRecord.getMemberId() != null ? String.valueOf(existingRecord.getMemberId())
                    : null;
            String newValue = data.memberId() != null ? String.valueOf(data.memberId()) : null;
            changedFields.put("memberId", new PersonChange.FieldChange(oldValue, newValue));
        }

        PersonChange.ChangeType type = changedFields.isEmpty() ? PersonChange.ChangeType.NO_CHANGE
                : PersonChange.ChangeType.UPDATE;

        return new PersonChange(type, existingRecord, data, changedFields);
    }

}
