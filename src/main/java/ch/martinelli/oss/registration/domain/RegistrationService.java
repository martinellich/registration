package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.registration.db.tables.records.*;
import ch.martinelli.oss.registration.mail.EmailSender;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static ch.martinelli.oss.registration.db.tables.EventRegistration.EVENT_REGISTRATION;
import static ch.martinelli.oss.registration.db.tables.RegistrationEmail.REGISTRATION_EMAIL;
import static ch.martinelli.oss.registration.db.tables.RegistrationEmailPerson.REGISTRATION_EMAIL_PERSON;

@Service
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final DSLContext dslContext;
    private final EmailSender emailSender;
    private final RegistrationEmailRepository registrationEmailRepository;
    private final PersonRepository personRepository;
    private final String publicAddress;

    public RegistrationService(RegistrationRepository registrationRepository, DSLContext dslContext,
                               EmailSender emailSender, RegistrationEmailRepository registrationEmailRepository,
                               PersonRepository personRepository, @Value("${public.address}") String publicAddress) {
        this.registrationRepository = registrationRepository;
        this.dslContext = dslContext;
        this.emailSender = emailSender;
        this.registrationEmailRepository = registrationEmailRepository;
        this.personRepository = personRepository;
        this.publicAddress = publicAddress;
    }

    @Transactional
    public void save(RegistrationRecord registration, Set<EventRecord> events, Set<PersonRecord> persons) {
        registrationRepository.saveWithEventsAndPersons(registration, events, persons);
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

        List<PersonRecord> persons = personRepository.findByRegistrationIdOrderByEmail(registration.getId());
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

    @Transactional
    public boolean sendMails(RegistrationRecord registration) {
        Integer count = dslContext.selectCount()
                .from(REGISTRATION_EMAIL)
                .where(REGISTRATION_EMAIL.REGISTRATION_ID.eq(registration.getId()))
                .and(REGISTRATION_EMAIL.SENT_AT.isNotNull())
                .fetchOneInto(Integer.class);
        if (count != null && count > 0) {
            return false;
        }

        Set<SimpleMailMessage> mails = new HashSet<>();

        List<RegistrationEmailViewRecord> registrationEmails = registrationEmailRepository.findByRegistrationId(registration.getId());
        for (RegistrationEmailViewRecord registrationEmail : registrationEmails) {
            SimpleMailMessage message = createMailMessage(registration, registrationEmail);
            mails.add(message);

            dslContext.update(REGISTRATION_EMAIL)
                    .set(REGISTRATION_EMAIL.SENT_AT, LocalDateTime.now())
                    .where(REGISTRATION_EMAIL.ID.eq(registrationEmail.getRegistrationEmailId()))
                    .execute();
        }

        emailSender.send(mails);

        return true;
    }

    private SimpleMailMessage createMailMessage(RegistrationRecord registration, RegistrationEmailViewRecord registrationEmail) {
        var message = new SimpleMailMessage();
        message.setFrom("jugi@tverlach.ch");
        message.setTo(registrationEmail.getEmail());
        message.setSubject("Jugi TV Erlach - Anmeldung für %d".formatted(registration.getYear()));
        message.setText("""
                Liebe Jugeler,
                
                Ab sofort kannst du dich für die Anlässe unter folgendem Link anmelden:
                
                %s/public/%s
                
                Viele Grüsse
                Jugi TV Erlach
                """.formatted(publicAddress, registrationEmail.getLink()));
        return message;
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

}
