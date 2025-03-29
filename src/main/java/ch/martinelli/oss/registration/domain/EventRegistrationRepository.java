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

// @formatter:off
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
        // First get all events ordered by date and title
        var events = dslContext
                .select(REGISTRATION_EVENT.event().fields())
                .from(REGISTRATION_EVENT)
                .where(REGISTRATION_EVENT.REGISTRATION_ID.eq(registrationId))
                .orderBy(REGISTRATION_EVENT.event().FROM_DATE, REGISTRATION_EVENT.event().TITLE)
                .fetchInto(EventRecord.class);

        // Create select fields starting with person info
        var fields = new ArrayList<SelectField<?>>();
        fields.add(PERSON.LAST_NAME);
        fields.add(PERSON.FIRST_NAME);

        // Add a field for each event
        for (var event : events) {
            Field<Boolean> registrationField = boolOr(
                    when(EVENT.TITLE.eq(event.getTitle()), EVENT_REGISTRATION.REGISTERED).otherwise(false)
            ).as(DSL.name(event.getTitle()));

            fields.add(registrationField);
        }

        return dslContext
                .select(fields)
                .from(PERSON)
                .join(EVENT_REGISTRATION).on(EVENT_REGISTRATION.PERSON_ID.eq(PERSON.ID))
                .join(EVENT).on(EVENT.ID.eq(EVENT_REGISTRATION.EVENT_ID))
                .where(EVENT_REGISTRATION.REGISTRATION_ID.eq(registrationId))
                .groupBy(PERSON.ID, PERSON.LAST_NAME, PERSON.FIRST_NAME)
                .orderBy(PERSON.LAST_NAME, PERSON.FIRST_NAME)
                .fetch()
                .map(EventRegistrationRow::fromRecord);
    }

    public int countRegistrationsByEvent(Long registrationId, String event) {
        return dslContext
                .fetchCount(dslContext.selectFrom(EVENT_REGISTRATION)
                        .where(EVENT_REGISTRATION.REGISTRATION_ID.eq(registrationId))
                        .and(EVENT_REGISTRATION.event().TITLE.eq(event))
                        .and(EVENT_REGISTRATION.REGISTERED.isTrue()));
    }

    public Optional<EventRegistrationRecord> findByRegistrationIdAndEventIdAndPersonId(Long registrationId, Long eventId, Long personId) {
        return dslContext
                .selectFrom(EVENT_REGISTRATION)
                .where(EVENT_REGISTRATION.REGISTRATION_ID.eq(registrationId))
                .and(EVENT_REGISTRATION.EVENT_ID.eq(eventId))
                .and(EVENT_REGISTRATION.PERSON_ID.eq(personId))
                .fetchOptional();
    }

}
