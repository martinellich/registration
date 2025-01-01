package ch.martinelli.oss.registration.domain;

import org.jooq.Record;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static ch.martinelli.oss.registration.db.tables.Person.PERSON;

public record EventRegistrationRow(
        String lastName,
        String firstName,
        Map<String, Boolean> registrations) {

    public static EventRegistrationRow fromRecord(Record record) {
        String lastName = record.get(PERSON.LAST_NAME);
        String firstName = record.get(PERSON.FIRST_NAME);

        Map<String, Boolean> registrations = new LinkedHashMap<>(); // Keep order
        Arrays.stream(record.fields())
                .filter(f -> f.getType() == Boolean.class)
                .forEach(f -> registrations.put(f.getName(), record.get(f, Boolean.class)));

        return new EventRegistrationRow(lastName, firstName, registrations);
    }
}