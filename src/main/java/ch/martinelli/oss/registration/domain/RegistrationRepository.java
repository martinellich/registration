package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.jooqspring.JooqDAO;
import ch.martinelli.oss.registration.db.tables.Registration;
import ch.martinelli.oss.registration.db.tables.records.EventRegistrationViewRecord;
import ch.martinelli.oss.registration.db.tables.records.RegistrationRecord;
import ch.martinelli.oss.registration.db.tables.records.RegistrationViewRecord;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static ch.martinelli.oss.registration.db.tables.EventRegistrationView.EVENT_REGISTRATION_VIEW;
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

    public List<EventRegistrationViewRecord> findAllEventRegistrationsFromView(Condition condition, int offset, int limit, List<OrderField<?>> orderBy) {
        return dslContext.selectFrom(EVENT_REGISTRATION_VIEW)
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
}
