package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.jooqspring.JooqDAO;
import ch.martinelli.oss.registration.db.tables.Registration;
import ch.martinelli.oss.registration.db.tables.records.EventRecord;
import ch.martinelli.oss.registration.db.tables.records.RegistrationRecord;
import ch.martinelli.oss.registration.db.tables.records.RegistrationViewRecord;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static ch.martinelli.oss.registration.db.tables.RegistrationEvent.REGISTRATION_EVENT;
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
}
