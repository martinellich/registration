package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.jooqspring.JooqDAO;
import ch.martinelli.oss.registration.db.tables.EventRegistration;
import ch.martinelli.oss.registration.db.tables.records.EventRecord;
import ch.martinelli.oss.registration.db.tables.records.EventRegistrationRecord;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SelectField;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ch.martinelli.oss.registration.db.tables.Event.EVENT;
import static ch.martinelli.oss.registration.db.tables.EventRegistration.EVENT_REGISTRATION;
import static ch.martinelli.oss.registration.db.tables.Person.PERSON;
import static ch.martinelli.oss.registration.db.tables.RegistrationEvent.REGISTRATION_EVENT;
import static org.jooq.impl.DSL.boolOr;
import static org.jooq.impl.DSL.when;

@Repository
public class EventRegistrationRepository extends JooqDAO<EventRegistration, EventRegistrationRecord, Long> {

    public EventRegistrationRepository(DSLContext dslContext) {
        super(dslContext, EVENT_REGISTRATION);
    }

    public Optional<EventRegistrationRecord> findByEventIdAndPersonId(Long eventId, Long personId) {
        return dslContext
                .selectFrom(EVENT_REGISTRATION)
                .where(EVENT_REGISTRATION.EVENT_ID.eq(eventId))
                .and(EVENT_REGISTRATION.PERSON_ID.eq(personId))
                .fetchOptional();
    }

    public List<EventRegistrationRow> getEventRegistrationMatrix(Long registrationId) {
        // First get all events ordered by date
        List<EventRecord> events = dslContext
                .select(REGISTRATION_EVENT.event().fields())
                .from(REGISTRATION_EVENT)
                .where(REGISTRATION_EVENT.REGISTRATION_ID.eq(registrationId))
                .orderBy(REGISTRATION_EVENT.event().FROM_DATE, REGISTRATION_EVENT.event().TITLE)
                .fetchInto(EventRecord.class);

        // Create select fields starting with person info
        List<SelectField<?>> fields = new ArrayList<>();
        fields.add(PERSON.LAST_NAME);
        fields.add(PERSON.FIRST_NAME);

        // Add a field for each event
        for (EventRecord event : events) {
            Field<Boolean> registrationField = boolOr(
                    when(EVENT.TITLE.eq(event.getTitle()), EVENT_REGISTRATION.REGISTERED)
                            .otherwise(false)
            ).as(DSL.name(event.getTitle().toLowerCase().replace(' ', '_')));

            fields.add(registrationField);
        }

        // Build and execute the query
        return dslContext
                .select(fields)
                .from(PERSON)
                .leftJoin(EVENT_REGISTRATION).on(EVENT_REGISTRATION.PERSON_ID.eq(PERSON.ID))
                .leftJoin(EVENT).on(EVENT.ID.eq(EVENT_REGISTRATION.EVENT_ID))
                .leftJoin(REGISTRATION_EVENT).on(EVENT.ID.eq(EVENT_REGISTRATION.EVENT_ID))
                .where(PERSON.ACTIVE.isTrue())
                .and(REGISTRATION_EVENT.REGISTRATION_ID.eq(registrationId))
                .groupBy(PERSON.ID, PERSON.LAST_NAME, PERSON.FIRST_NAME)
                .orderBy(PERSON.LAST_NAME, PERSON.FIRST_NAME)
                .fetch()
                .map(EventRegistrationRow::fromRecord);
    }
}
