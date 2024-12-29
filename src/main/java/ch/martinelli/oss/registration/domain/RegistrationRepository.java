package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.jooqspring.JooqDAO;
import ch.martinelli.oss.registration.db.tables.Registration;
import ch.martinelli.oss.registration.db.tables.records.EventRegistrationViewRecord;
import ch.martinelli.oss.registration.db.tables.records.RegistrationRecord;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.springframework.stereotype.Repository;

import java.util.List;

import static ch.martinelli.oss.registration.db.tables.EventRegistrationView.EVENT_REGISTRATION_VIEW;

@Repository
public class RegistrationRepository extends JooqDAO<Registration, RegistrationRecord, Long> {

    public RegistrationRepository(DSLContext dslContext) {
        super(dslContext, Registration.REGISTRATION);
    }

    public List<EventRegistrationViewRecord> findAllFromView(Condition condition, int offset, int limit, List<OrderField<?>> orderBy) {
        return dslContext.selectFrom(EVENT_REGISTRATION_VIEW)
                .where(condition)
                .orderBy(orderBy)
                .offset(offset)
                .limit(limit)
                .fetch();
    }
}
