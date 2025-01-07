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
    private final String sender;

    public RegistrationService(RegistrationRepository registrationRepository, DSLContext dslContext,
                               EmailSender emailSender, RegistrationEmailRepository registrationEmailRepository,
                               PersonRepository personRepository,
                               @Value("${public.address}") String publicAddress,
                               @Value("${spring.mail.username}") String sender) {
        this.registrationRepository = registrationRepository;
        this.dslContext = dslContext;
        this.emailSender = emailSender;
        this.registrationEmailRepository = registrationEmailRepository;
        this.personRepository = personRepository;
        this.publicAddress = publicAddress;
        this.sender = sender;
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

    @Transactional
    public boolean sendMails(RegistrationRecord registration) {
        Set<SimpleMailMessage> mails = new HashSet<>();

        List<RegistrationEmailViewRecord> registrationEmails = registrationEmailRepository.findByRegistrationIdAndSentAtIsNull(registration.getId());
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
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(registrationEmail.getEmail());
        message.setSubject("%s %d".formatted(registration.getTitle(), registration.getYear()));
        String url = "%s/public/%s".formatted(publicAddress, registrationEmail.getLink());
        message.setText(registration.getEmailText().formatted(url));
        return message;
    }
}
