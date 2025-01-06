package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.jooqspring.JooqDAO;
import ch.martinelli.oss.registration.db.tables.Registration;
import ch.martinelli.oss.registration.db.tables.records.EventRecord;
import ch.martinelli.oss.registration.db.tables.records.PersonRecord;
import ch.martinelli.oss.registration.db.tables.records.RegistrationRecord;
import ch.martinelli.oss.registration.db.tables.records.RegistrationViewRecord;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ch.martinelli.oss.registration.db.tables.RegistrationEvent.REGISTRATION_EVENT;
import static ch.martinelli.oss.registration.db.tables.RegistrationPerson.REGISTRATION_PERSON;
import static ch.martinelli.oss.registration.db.tables.RegistrationView.REGISTRATION_VIEW;

@Repository
public class RegistrationRepository extends JooqDAO<Registration, RegistrationRecord, Long> {

    public RegistrationRepository(DSLContext dslContext) {
        super(dslContext, Registration.REGISTRATION);
    }

    public List<RegistrationViewRecord> findAllFromView(Condition condition, int offset, int limit, List<OrderField<?>> orderBy) {
        return dslContext.selectFrom(REGISTRATION_VIEW)
                .where(condition)
                .orderBy(orderBy)
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    public int countFromView(Condition condition) {
        return dslContext.fetchCount(dslContext.selectFrom(REGISTRATION_VIEW).where(condition));
    }

    public Optional<RegistrationViewRecord> findByIdFromView(Long registrationId) {
        return dslContext.selectFrom(REGISTRATION_VIEW)
                .where(REGISTRATION_VIEW.ID.eq(registrationId))
                .fetchOptional();
    }

    public List<EventRecord> findAllEventsByRegistrationId(Long registrationId) {
        return dslContext.
                select(REGISTRATION_EVENT.event().fields())
                .from(REGISTRATION_EVENT)
                .where(REGISTRATION_EVENT.REGISTRATION_ID.eq(registrationId))
                .orderBy(REGISTRATION_EVENT.event().FROM_DATE)
                .fetchInto(EventRecord.class);
    }

    @SuppressWarnings("java:S6809")
    @Transactional
    public void saveWithEventsAndPersons(RegistrationRecord registration, Set<EventRecord> events, Set<PersonRecord> persons) {
        save(registration);

        dslContext
                .deleteFrom(REGISTRATION_EVENT)
                .where(REGISTRATION_EVENT.REGISTRATION_ID.eq(registration.getId()))
                .execute();

        events.forEach(event -> dslContext.insertInto(REGISTRATION_EVENT,
                        REGISTRATION_EVENT.REGISTRATION_ID, REGISTRATION_EVENT.EVENT_ID)
                .values(registration.getId(), event.getId())
                .execute());

        dslContext
                .deleteFrom(REGISTRATION_PERSON)
                .where(REGISTRATION_PERSON.REGISTRATION_ID.eq(registration.getId()))
                .execute();

        persons.forEach(person ->
                dslContext.insertInto(REGISTRATION_PERSON,
                                REGISTRATION_PERSON.REGISTRATION_ID, REGISTRATION_PERSON.PERSON_ID)
                        .values(registration.getId(), person.getId())
                        .execute());
    }

}
