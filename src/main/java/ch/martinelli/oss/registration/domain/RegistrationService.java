package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.registration.db.tables.records.*;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static ch.martinelli.oss.registration.db.tables.RegistrationEmail.REGISTRATION_EMAIL;
import static ch.martinelli.oss.registration.db.tables.RegistrationEmailPerson.REGISTRATION_EMAIL_PERSON;
import static ch.martinelli.oss.registration.db.tables.RegistrationEvent.REGISTRATION_EVENT;
import static ch.martinelli.oss.registration.db.tables.RegistrationPerson.REGISTRATION_PERSON;

@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);
    private final RegistrationRepository registrationRepository;
    private final DSLContext dslContext;
    private final JavaMailSender mailSender;

    public RegistrationService(RegistrationRepository registrationRepository, DSLContext dslContext, JavaMailSender mailSender) {
        this.registrationRepository = registrationRepository;
        this.dslContext = dslContext;
        this.mailSender = mailSender;
    }

    @Transactional
    public void save(RegistrationRecord registration, Set<EventRecord> events, Set<PersonRecord> persons) {
        registrationRepository.save(registration);

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

    public Set<EventRecord> findEventsByRegistration(Long registrationId) {
        List<EventRecord> events = dslContext
                .select(REGISTRATION_EVENT.event().fields())
                .from(REGISTRATION_EVENT)
                .where(REGISTRATION_EVENT.REGISTRATION_ID.eq(registrationId))
                .fetchInto(EventRecord.class);
        return new HashSet<>(events);
    }

    public Set<PersonRecord> findPersonsByRegistrationId(Long registrationId) {
        List<PersonRecord> persons = findPersonRByRegistrationIdOrderByEmail(registrationId);
        return new HashSet<>(persons);
    }

    @Transactional
    public boolean createMailing(RegistrationRecord registration) {
        Integer count = dslContext.selectCount()
                .from(REGISTRATION_EMAIL)
                .where(REGISTRATION_EMAIL.REGISTRATION_ID.eq(registration.getId()))
                .fetchOneInto(Integer.class);
        if (count != null && count > 0) {
            return false;
        }

        List<PersonRecord> persons = findPersonRByRegistrationIdOrderByEmail(registration.getId());
        RegistrationEmailRecord registrationEmail = null;
        for (PersonRecord person : persons) {
            if (registrationEmail == null || !registrationEmail.getEmail().equals(person.getEmail())) {
                registrationEmail = dslContext.newRecord(REGISTRATION_EMAIL);
                registrationEmail.setEmail(person.getEmail());
                registrationEmail.setLink(UUID.randomUUID().toString().replace("-", ""));
                registrationEmail.setRegistrationId(registration.getId());
                registrationEmail.store();
            }
            RegistrationEmailPersonRecord registrationEmailPerson = dslContext.newRecord(REGISTRATION_EMAIL_PERSON);
            registrationEmailPerson.setRegistrationEmailId(registrationEmail.getId());
            registrationEmailPerson.setPersonId(person.getId());
            registrationEmailPerson.store();
        }
        return true;
    }

    public void sendMails() {
        var message = new SimpleMailMessage();
        message.setFrom("jugi@tverlach.ch");
        message.setTo("simon@martinelli.ch");
        message.setSubject("Test Subject");
        message.setText("Test Text");
        log.info("Start sending");
        mailSender.send(message);
        log.info("Message sent");
    }

    private List<PersonRecord> findPersonRByRegistrationIdOrderByEmail(Long registrationId) {
        return dslContext
                .select(REGISTRATION_PERSON.person().fields())
                .from(REGISTRATION_PERSON)
                .where(REGISTRATION_PERSON.REGISTRATION_ID.eq(registrationId))
                .fetchInto(PersonRecord.class);
    }
}
