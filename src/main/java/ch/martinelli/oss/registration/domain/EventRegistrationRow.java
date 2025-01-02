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

    public static EventRegistrationRow fromRecord(Record jooqRecord) {
        String lastName = jooqRecord.get(PERSON.LAST_NAME);
        String firstName = jooqRecord.get(PERSON.FIRST_NAME);

        Map<String, Boolean> registrations = new LinkedHashMap<>(); // Keep order
        Arrays.stream(jooqRecord.fields())
                .filter(f -> f.getType() == Boolean.class)
                .forEach(f -> registrations.put(f.getName(), jooqRecord.get(f, Boolean.class)));

        return new EventRegistrationRow(lastName, firstName, registrations);
    }
}