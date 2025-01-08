package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.registration.db.tables.records.*;
import ch.martinelli.oss.registration.mail.EmailSender;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static ch.martinelli.oss.registration.db.tables.EventRegistration.EVENT_REGISTRATION;
import static ch.martinelli.oss.registration.db.tables.RegistrationEmail.REGISTRATION_EMAIL;
import static ch.martinelli.oss.registration.db.tables.RegistrationEmailPerson.REGISTRATION_EMAIL_PERSON;

@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);
    private final RegistrationRepository registrationRepository;
    private final DSLContext dslContext;
    private final EmailSender emailSender;
    private final RegistrationEmailRepository registrationEmailRepository;
    private final PersonRepository personRepository;

    public RegistrationService(RegistrationRepository registrationRepository, DSLContext dslContext,
                               EmailSender emailSender, RegistrationEmailRepository registrationEmailRepository,
                               PersonRepository personRepository) {
        this.registrationRepository = registrationRepository;
        this.dslContext = dslContext;
        this.emailSender = emailSender;
        this.registrationEmailRepository = registrationEmailRepository;
        this.personRepository = personRepository;
    }

    @Transactional
    public void save(RegistrationRecord registration, Set<EventRecord> events, Set<PersonRecord> persons) {
        registrationRepository.saveWithEventsAndPersons(registration, events, persons);
    }

    @Transactional
    public boolean createMailing(RegistrationRecord registration) {
        List<PersonRecord> persons = personRepository.findByRegistrationIdOrderByEmail(registration.getId());
        RegistrationEmailRecord registrationEmail = null;
        for (PersonRecord person : persons) {
            if (!registrationEmailRepository.exitsByRegistrationIdAndEmail(registration.getId(), person.getEmail())) {
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
        }
        return true;
    }

    @Transactional
    public void register(Set<EventRegistrationRecord> eventRegistrations) {
        for (EventRegistrationRecord eventRegistration : eventRegistrations) {
            Optional<EventRegistrationRecord> existingEventRegistration = dslContext
                    .selectFrom(EVENT_REGISTRATION)
                    .where(EVENT_REGISTRATION.REGISTRATION_ID.eq(eventRegistration.getRegistrationId()))
                    .and(EVENT_REGISTRATION.EVENT_ID.eq(eventRegistration.getEventId()))
                    .and(EVENT_REGISTRATION.PERSON_ID.eq(eventRegistration.getPersonId()))
                    .fetchOptional();
            if (existingEventRegistration.isPresent()) {
                EventRegistrationRecord eventRegistrationRecord = existingEventRegistration.get();
                eventRegistrationRecord.setRegistered(eventRegistration.getRegistered());
                eventRegistrationRecord.store();
            } else {
                dslContext.attach(eventRegistration);
                eventRegistration.store();
            }
        }
    }

    @Async
    public void sendMails(RegistrationRecord registration) {
        List<RegistrationEmailViewRecord> registrationEmails = registrationEmailRepository.findByRegistrationIdAndSentAtIsNull(registration.getId());
        for (RegistrationEmailViewRecord registrationEmail : registrationEmails) {
            try {
                emailSender.sendEmail(registration, registrationEmail);
            } catch (Exception e) {
                log.error("Error sending email", e);
            }
        }
    }
}
