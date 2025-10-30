package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.registration.db.tables.records.PersonRecord;

import java.util.Map;

/**
 * Represents a detected change when importing persons from Excel. Can represent either a
 * new person to be added or an update to an existing person.
 */
public class PersonChange {

    public enum ChangeType {

        NEW, UPDATE, DEACTIVATE, NO_CHANGE

    }

    private final ChangeType type;

    private final PersonRecord existingRecord;

    private final ExcelPersonData newData;

    private final Map<String, FieldChange> changedFields;

    private boolean accepted;

    public PersonChange(ChangeType type, PersonRecord existingRecord, ExcelPersonData newData,
            Map<String, FieldChange> changedFields) {
        this.type = type;
        this.existingRecord = existingRecord;
        this.newData = newData;
        this.changedFields = changedFields;
        this.accepted = true; // Default to accepted
    }

    public ChangeType getType() {
        return type;
    }

    public PersonRecord getExistingRecord() {
        return existingRecord;
    }

    public ExcelPersonData getNewData() {
        return newData;
    }

    public Map<String, FieldChange> getChangedFields() {
        return changedFields;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    /**
     * Represents a change in a single field from old value to new value.
     */
    public record FieldChange(String oldValue, String newValue) {
        @Override
        public String toString() {
            if (oldValue == null || oldValue.isEmpty()) {
                return newValue;
            }
            return oldValue + " → " + newValue;
        }
    }

}
