package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.registration.db.tables.records.*;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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

    private final EventRegistrationRepository eventRegistrationRepository;

    public RegistrationService(RegistrationRepository registrationRepository, DSLContext dslContext,
            EmailSender emailSender, RegistrationEmailRepository registrationEmailRepository,
            PersonRepository personRepository, EventRegistrationRepository eventRegistrationRepository) {
        this.registrationRepository = registrationRepository;
        this.dslContext = dslContext;
        this.emailSender = emailSender;
        this.registrationEmailRepository = registrationEmailRepository;
        this.personRepository = personRepository;
        this.eventRegistrationRepository = eventRegistrationRepository;
    }

    @Transactional
    public void save(RegistrationRecord registration, Set<EventRecord> events, Set<PersonRecord> persons) {
        registrationRepository.saveWithEventsAndPersons(registration, events, persons);
    }

    @Transactional
    public boolean createMailing(RegistrationRecord registration) {
        List<PersonRecord> persons = personRepository.findByRegistrationId(registration.getId());
        for (PersonRecord person : persons) {
            Optional<RegistrationEmailRecord> optionalRegistrationEmail = registrationEmailRepository
                .findByRegistrationIdAndEmail(registration.getId(), person.getEmail());
            RegistrationEmailRecord registrationEmail;
            if (optionalRegistrationEmail.isEmpty()) {
                registrationEmail = dslContext.newRecord(REGISTRATION_EMAIL);
                registrationEmail.setEmail(person.getEmail());
                registrationEmail.setLink(UUID.randomUUID().toString().replace("-", ""));
                registrationEmail.setRegistrationId(registration.getId());
                registrationEmail.store();
            }
            else {
                registrationEmail = optionalRegistrationEmail.get();
            }
            Optional<RegistrationEmailPersonRecord> optionalRegistrationEmailPerson = registrationEmailRepository
                .findByRegistrationEmailIdAndPersonId(registrationEmail.getId(), person.getId());
            if (optionalRegistrationEmailPerson.isEmpty()) {
                RegistrationEmailPersonRecord registrationEmailPerson = dslContext.newRecord(REGISTRATION_EMAIL_PERSON);
                registrationEmailPerson.setRegistrationEmailId(registrationEmail.getId());
                registrationEmailPerson.setPersonId(person.getId());
                registrationEmailPerson.store();
            }
        }
        return true;
    }

    @Transactional
    public void register(Long registrationEmailId, Set<EventRegistrationRecord> eventRegistrations) {
        if (!eventRegistrations.isEmpty()) {
            registrationEmailRepository.findById(registrationEmailId).ifPresent(registrationEmail -> {
                registrationEmail.setRegisteredAt(LocalDateTime.now());
                registrationEmail.store();
            });

            for (EventRegistrationRecord eventRegistration : eventRegistrations) {
                Optional<EventRegistrationRecord> existingEventRegistration = eventRegistrationRepository
                    .findByRegistrationIdAndEventIdAndPersonId(eventRegistration.getRegistrationId(),
                            eventRegistration.getEventId(), eventRegistration.getPersonId());
                if (existingEventRegistration.isPresent()) {
                    EventRegistrationRecord eventRegistrationRecord = existingEventRegistration.get();
                    eventRegistrationRecord.setRegistered(eventRegistration.getRegistered());
                    eventRegistrationRecord.store();
                }
                else {
                    dslContext.attach(eventRegistration);
                    eventRegistration.store();
                }
            }
        }
    }

    @Async
    public void sendMails(RegistrationRecord registration, String replayTo) {
        List<RegistrationEmailViewRecord> registrationEmails = registrationEmailRepository
            .findByRegistrationIdAndSentAtIsNull(registration.getId());
        for (RegistrationEmailViewRecord registrationEmail : registrationEmails) {
            try {
                emailSender.sendEmail(registration, registrationEmail, replayTo);
            }
            catch (Exception e) {
                log.error("Error sending email", e);
            }
        }
    }

}
