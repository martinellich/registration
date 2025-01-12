package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.jooqspring.JooqDAO;
import ch.martinelli.oss.registration.db.tables.Person;
import ch.martinelli.oss.registration.db.tables.records.PersonRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;

import static ch.martinelli.oss.registration.db.tables.RegistrationPerson.REGISTRATION_PERSON;

@Repository
public class PersonRepository extends JooqDAO<Person, PersonRecord, Long> {

    public PersonRepository(DSLContext dslContext) {
        super(dslContext, Person.PERSON);
    }

    public List<PersonRecord> findByRegistrationId(Long registrationId) {
        return dslContext
                .select(REGISTRATION_PERSON.person().fields())
                .from(REGISTRATION_PERSON)
                .where(REGISTRATION_PERSON.REGISTRATION_ID.eq(registrationId))
                .fetchInto(PersonRecord.class);
    }

}
