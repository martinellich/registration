package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.registration.db.tables.records.*;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

    private final EventRepository eventRepository;

    private final String publicAddress;

    public RegistrationService(RegistrationRepository registrationRepository, DSLContext dslContext,
            EmailSender emailSender, RegistrationEmailRepository registrationEmailRepository,
            PersonRepository personRepository, EventRegistrationRepository eventRegistrationRepository,
            EventRepository eventRepository, @Value("${registration.public.address}") String publicAddress) {
        this.registrationRepository = registrationRepository;
        this.dslContext = dslContext;
        this.emailSender = emailSender;
        this.registrationEmailRepository = registrationEmailRepository;
        this.personRepository = personRepository;
        this.eventRegistrationRepository = eventRegistrationRepository;
        this.eventRepository = eventRepository;
        this.publicAddress = publicAddress;
    }

    @Transactional
    public void save(RegistrationRecord registration, Set<EventRecord> events, Set<PersonRecord> persons) {
        registrationRepository.saveWithEventsAndPersons(registration, events, persons);
    }

    @Transactional
    public boolean createMailing(RegistrationRecord registration) {
        var persons = personRepository.findByRegistrationId(registration.getId());
        for (var person : persons) {
            var optionalRegistrationEmail = registrationEmailRepository
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
            var optionalRegistrationEmailPerson = registrationEmailRepository
                .findByRegistrationEmailIdAndPersonId(registrationEmail.getId(), person.getId());
            if (optionalRegistrationEmailPerson.isEmpty()) {
                var registrationEmailPerson = dslContext.newRecord(REGISTRATION_EMAIL_PERSON);
                registrationEmailPerson.setRegistrationEmailId(registrationEmail.getId());
                registrationEmailPerson.setPersonId(person.getId());
                registrationEmailPerson.store();
            }
        }
        return true;
    }

    @Transactional
    public boolean register(Long registrationEmailId, Set<EventRegistrationRecord> eventRegistrations) {
        if (eventRegistrations.isEmpty()) {
            return false;
        }

        // Fetch all existing registrations to compare
        var existingRegistrations = eventRegistrations.stream()
            .map(eventReg -> eventRegistrationRepository.findByRegistrationIdAndEventIdAndPersonId(
                    eventReg.getRegistrationId(), eventReg.getEventId(), eventReg.getPersonId()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());

        // Check if there are any changes
        if (!hasRegistrationChanged(existingRegistrations, eventRegistrations)) {
            // No changes detected, skip save and email
            return false;
        }

        // Track if this is the first registration
        boolean isFirstRegistration = false;
        var registrationEmailOpt = registrationEmailRepository.findById(registrationEmailId);
        if (registrationEmailOpt.isPresent()) {
            var registrationEmail = registrationEmailOpt.get();
            isFirstRegistration = registrationEmail.getRegisteredAt() == null;
            registrationEmail.setRegisteredAt(LocalDateTime.now());
            registrationEmail.store();
        }

        for (var eventRegistration : eventRegistrations) {
            var existingEventRegistration = eventRegistrationRepository.findByRegistrationIdAndEventIdAndPersonId(
                    eventRegistration.getRegistrationId(), eventRegistration.getEventId(),
                    eventRegistration.getPersonId());
            if (existingEventRegistration.isPresent()) {
                var eventRegistrationRecord = existingEventRegistration.get();
                eventRegistrationRecord.setRegistered(eventRegistration.getRegistered());
                eventRegistrationRecord.store();
            }
            else {
                dslContext.attach(eventRegistration);
                eventRegistration.store();
            }
        }

        // Send confirmation email
        sendConfirmationEmail(registrationEmailId, isFirstRegistration);

        return true;
    }

    @Async
    public void sendMails(RegistrationRecord registration, String replayTo) {
        var registrationEmails = registrationEmailRepository.findByRegistrationIdAndSentAtIsNull(registration.getId());
        for (var registrationEmail : registrationEmails) {
            try {
                emailSender.sendEmail(registration, registrationEmail, replayTo);
            }
            catch (Exception e) {
                log.error("Error sending email", e);
            }
        }
    }

    private void sendConfirmationEmail(Long registrationEmailId, boolean isFirstRegistration) {
        var registrationEmailViewOpt = registrationEmailRepository.findByIdFromView(registrationEmailId);
        if (registrationEmailViewOpt.isEmpty()) {
            log.warn("Could not find registration email with id {}", registrationEmailId);
            return;
        }

        var registrationEmailView = registrationEmailViewOpt.get();
        var registrationOpt = registrationRepository.findById(registrationEmailView.getRegistrationId());
        if (registrationOpt.isEmpty()) {
            log.warn("Could not find registration with id {}", registrationEmailView.getRegistrationId());
            return;
        }

        var registration = registrationOpt.get();

        // Choose template based on whether it's first registration or update
        String subject;
        String template;
        if (isFirstRegistration) {
            subject = registration.getConfirmationEmailSubjectNew();
            template = registration.getConfirmationEmailTextNew();
        }
        else {
            subject = registration.getConfirmationEmailSubjectUpdate();
            template = registration.getConfirmationEmailTextUpdate();
        }

        // If no template is configured, skip sending
        if (template == null || template.isBlank()) {
            log.info("No confirmation email template configured for registration {}", registration.getId());
            return;
        }

        // Build person names list
        var persons = registrationEmailRepository.findPersonsByRegistrationEmailId(registrationEmailId);
        var personNames = persons.stream()
            .map(p -> p.getFirstName() + " " + p.getLastName())
            .collect(Collectors.joining("\n- ", "- ", ""));

        // Build events list with registration status
        var events = eventRepository.findByRegistrationId(registration.getId());
        var eventsText = new StringBuilder();
        for (var event : events) {
            eventsText.append("- ").append(event.getTitle()).append(":\n");
            for (var person : persons) {
                var eventRegOpt = eventRegistrationRepository
                    .findByRegistrationIdAndEventIdAndPersonId(registration.getId(), event.getId(), person.getId());
                boolean registered = eventRegOpt.map(EventRegistrationRecord::getRegistered).orElse(false);
                eventsText.append("  ")
                    .append(person.getFirstName())
                    .append(" ")
                    .append(person.getLastName())
                    .append(": ")
                    .append(registered ? "Ja" : "Nein")
                    .append("\n");
            }
        }

        // Format dates
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        var openFrom = registration.getOpenFrom().format(dateFormatter);
        var openUntil = registration.getOpenUntil().format(dateFormatter);

        // Build magic link
        var magicLink = "%s/public/%s".formatted(publicAddress, registrationEmailView.getLink());

        // Replace placeholders
        var body = template.replace("%PERSON_NAMES%", personNames)
            .replace("%EVENTS%", eventsText.toString().trim())
            .replace("%LINK%", magicLink)
            .replace("%OPEN_FROM%", openFrom)
            .replace("%OPEN_UNTIL%", openUntil)
            .replace("%REMARKS%", registration.getRemarks() != null ? registration.getRemarks() : "");

        // Send email
        emailSender.sendConfirmationEmail(registrationEmailView.getEmail(),
                subject != null ? subject : "Anmeldebestätigung", body, registrationEmailView.getEmail() // replyTo
                                                                                                         // same
                                                                                                         // as
                                                                                                         // recipient
        );
    }

    private boolean hasRegistrationChanged(Set<EventRegistrationRecord> existing,
            Set<EventRegistrationRecord> submitted) {
        // If no existing registrations, this is the first time - consider it a change
        if (existing.isEmpty()) {
            return true;
        }

        // Compare each submitted registration with existing
        for (var submittedReg : submitted) {
            var matchingExisting = existing.stream()
                .filter(e -> e.getRegistrationId().equals(submittedReg.getRegistrationId())
                        && e.getEventId().equals(submittedReg.getEventId())
                        && e.getPersonId().equals(submittedReg.getPersonId()))
                .findFirst();

            if (matchingExisting.isEmpty()) {
                // New registration (not in existing) - this is a change
                return true;
            }

            // Check if registered status changed
            if (!matchingExisting.get().getRegistered().equals(submittedReg.getRegistered())) {
                return true;
            }
        }

        // No changes detected
        return false;
    }

}
