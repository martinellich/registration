package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.jooqspring.JooqDAO;
import ch.martinelli.oss.registration.db.tables.Person;
import ch.martinelli.oss.registration.db.tables.records.PersonRecord;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.List;

import static ch.martinelli.oss.registration.db.tables.Person.PERSON;
import static ch.martinelli.oss.registration.db.tables.RegistrationPerson.REGISTRATION_PERSON;

// @formatter:off
@Repository
public class PersonRepository extends JooqDAO<Person, PersonRecord, Long> {

    public PersonRepository(DSLContext dslContext) {
        super(dslContext, PERSON);
    }

    public List<PersonRecord> findByRegistrationId(Long registrationId) {
        return dslContext
                .select(REGISTRATION_PERSON.person().fields())
                .from(REGISTRATION_PERSON)
                .where(REGISTRATION_PERSON.REGISTRATION_ID.eq(registrationId))
                .fetchInto(PersonRecord.class);
    }

    public List<PersonRecord> findAll(boolean hideInactive, int offset, int limit, List<OrderField<?>> orderBy) {
        SelectConditionStep<Record> selectFrom = dslContext
                .select(PERSON.fields())
                .from(PERSON)
                .where(DSL.noCondition());
        if (hideInactive) {
            selectFrom = selectFrom.and(PERSON.ACTIVE.eq(true));
        }
        return selectFrom
                .orderBy(orderBy)
                .offset(offset)
                .limit(limit)
                .fetchInto(PersonRecord.class);
    }

}
