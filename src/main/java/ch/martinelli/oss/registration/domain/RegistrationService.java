package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.registration.db.tables.records.EventRecord;
import ch.martinelli.oss.registration.db.tables.records.PersonRecord;
import ch.martinelli.oss.registration.db.tables.records.RegistrationRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ch.martinelli.oss.registration.db.tables.RegistrationEvent.REGISTRATION_EVENT;
import static ch.martinelli.oss.registration.db.tables.RegistrationPerson.REGISTRATION_PERSON;

@Service
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final DSLContext dslContext;

    public RegistrationService(RegistrationRepository registrationRepository, DSLContext dslContext) {
        this.registrationRepository = registrationRepository;
        this.dslContext = dslContext;
    }

    @Transactional
    public void save(RegistrationRecord registration, Set<EventRecord> events, Set<PersonRecord> persons) {
        registrationRepository.save(registration);

        dslContext.deleteFrom(REGISTRATION_EVENT)
                .where(REGISTRATION_EVENT.REGISTRATION_ID.eq(registration.getId()))
                .execute();

        events.forEach(event -> dslContext.insertInto(REGISTRATION_EVENT,
                        REGISTRATION_EVENT.REGISTRATION_ID, REGISTRATION_EVENT.EVENT_ID)
                .values(registration.getId(), event.getId())
                .execute());

        dslContext.deleteFrom(REGISTRATION_PERSON)
                .where(REGISTRATION_PERSON.REGISTRATION_ID.eq(registration.getId()))
                .execute();

        persons.forEach(person ->
                dslContext.insertInto(REGISTRATION_PERSON,
                                REGISTRATION_PERSON.REGISTRATION_ID, REGISTRATION_PERSON.PERSON_ID)
                        .values(registration.getId(), person.getId())
                        .execute());
    }

    public Set<EventRecord> findEventsByRegistration(Long registrationId) {
        List<EventRecord> events = dslContext.select(REGISTRATION_EVENT.event().fields())
                .from(REGISTRATION_EVENT)
                .where(REGISTRATION_EVENT.REGISTRATION_ID.eq(registrationId))
                .fetchInto(EventRecord.class);
        return new HashSet<>(events);
    }

    public Set<PersonRecord> findPersonsByRegistration(Long registrationId) {
        List<PersonRecord> persons = dslContext.select(REGISTRATION_PERSON.person().fields())
                .from(REGISTRATION_PERSON)
                .where(REGISTRATION_PERSON.REGISTRATION_ID.eq(registrationId))
                .fetchInto(PersonRecord.class);
        return new HashSet<>(persons);
    }
}
