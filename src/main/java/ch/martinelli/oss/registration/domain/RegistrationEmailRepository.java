package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.jooqspring.JooqDAO;
import ch.martinelli.oss.registration.db.tables.RegistrationEmail;
import ch.martinelli.oss.registration.db.tables.records.PersonRecord;
import ch.martinelli.oss.registration.db.tables.records.RegistrationEmailRecord;
import ch.martinelli.oss.registration.db.tables.records.RegistrationEmailViewRecord;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static ch.martinelli.oss.registration.db.tables.RegistrationEmail.REGISTRATION_EMAIL;
import static ch.martinelli.oss.registration.db.tables.RegistrationEmailPerson.REGISTRATION_EMAIL_PERSON;
import static ch.martinelli.oss.registration.db.tables.RegistrationEmailView.REGISTRATION_EMAIL_VIEW;

@Repository
public class RegistrationEmailRepository extends JooqDAO<RegistrationEmail, RegistrationEmailRecord, Long> {

    public RegistrationEmailRepository(DSLContext dslContext) {
        super(dslContext, REGISTRATION_EMAIL);
    }

    public List<RegistrationEmailViewRecord> findAllFromView(Condition filter, int offset, int limit, List<OrderField<?>> orderFields) {
        return dslContext
                .selectFrom(REGISTRATION_EMAIL_VIEW)
                .where(filter)
                .orderBy(orderFields)
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    public List<RegistrationEmailViewRecord> findByRegistrationId(Long id) {
        return dslContext
                .selectFrom(REGISTRATION_EMAIL_VIEW)
                .where(REGISTRATION_EMAIL_VIEW.REGISTRATION_ID.eq(id))
                .fetch();
    }

    public Optional<RegistrationEmailRecord> findByLink(String parameter) {
        return dslContext
                .selectFrom(REGISTRATION_EMAIL)
                .where(REGISTRATION_EMAIL.LINK.eq(parameter))
                .fetchOptional();
    }

    public List<PersonRecord> findPersonsByRegistrationEmailId(Long id) {
        return dslContext
                .select(REGISTRATION_EMAIL_PERSON.person().fields())
                .from(REGISTRATION_EMAIL_PERSON)
                .where(REGISTRATION_EMAIL_PERSON.REGISTRATION_EMAIL_ID.eq(id))
                .fetchInto(PersonRecord.class);
    }
}
