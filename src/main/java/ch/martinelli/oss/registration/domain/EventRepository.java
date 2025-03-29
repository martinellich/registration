package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.jooqspring.JooqDAO;
import ch.martinelli.oss.registration.db.tables.Event;
import ch.martinelli.oss.registration.db.tables.records.EventRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.Set;

import static ch.martinelli.oss.registration.db.tables.RegistrationEvent.REGISTRATION_EVENT;

// @formatter:off
@Repository
public class EventRepository extends JooqDAO<Event, EventRecord, Long> {

    public EventRepository(DSLContext dslContext) {
        super(dslContext, Event.EVENT);
    }

    public Set<EventRecord> findByRegistrationId(Long registrationId) {
        var events = dslContext
                .select(REGISTRATION_EVENT.event().fields())
                .from(REGISTRATION_EVENT)
                .where(REGISTRATION_EVENT.REGISTRATION_ID.eq(registrationId))
                .fetchInto(EventRecord.class);
        return new HashSet<>(events);
    }

}
