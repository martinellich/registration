package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.jooqspring.JooqDAO;
import ch.martinelli.oss.registration.db.tables.Event;
import ch.martinelli.oss.registration.db.tables.records.EventRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
public class EventRepository extends JooqDAO<Event, EventRecord, Long> {

    public EventRepository(DSLContext dslContext) {
        super(dslContext, Event.EVENT);
    }
}
