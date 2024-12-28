package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.jooqspring.JooqDAO;
import ch.martinelli.oss.registration.db.tables.Person;
import ch.martinelli.oss.registration.db.tables.records.PersonRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
public class PersonRepository extends JooqDAO<Person, PersonRecord, Long> {

    public PersonRepository(DSLContext dslContext) {
        super(dslContext, Person.PERSON);
    }
}
