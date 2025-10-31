package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.registration.TestcontainersConfiguration;
import ch.martinelli.oss.registration.db.tables.records.EventRegistrationRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import skydrinker.testcontainers.mailcatcher.MailCatcherContainer;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RegistrationServiceTest {

    @Container
    static final MailCatcherContainer mailcatcherContainer = new MailCatcherContainer();

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private RegistrationEmailRepository registrationEmailRepository;

    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        mailcatcherContainer.start();

        registry.add("spring.mail.host", mailcatcherContainer::getHost);
        registry.add("spring.mail.port", mailcatcherContainer::getSmtpPort);
        registry.add("spring.mail.username", () -> "test@example.com");
        registry.add("spring.mail.password", () -> "pass");
        registry.add("registration.public.address", () -> "https://anmeldungen.tverlach.ch");
    }

    @AfterAll
    static void afterAll() {
        mailcatcherContainer.stop();
    }

    @BeforeEach
    void setUp() {
        // Clear email container before each test
        mailcatcherContainer.getAllEmails();
    }

    @Test
    void register_first_time_sends_new_confirmation_email() {
        // Given: Registration email ID 3 with person 2 (Barry Rodriquez)
        // Registration 3 has events 4 (CIS 2025) and 5 (Jugendmeisterschaft 2025)
        // Note: Registration 3 has simple templates without %LINK% placeholder
        var registrationEmailId = 3L;
        var registrationId = 3L;
        var personId = 2L;
        var event4Id = 4L;
        var event5Id = 5L;

        var eventRegistrations = Set.of(createEventRegistration(registrationId, event4Id, personId, true),
                createEventRegistration(registrationId, event5Id, personId, false));

        // When: Register for the first time
        registrationService.register(registrationEmailId, eventRegistrations);

        // Then: Confirmation email should be sent
        // Verify email content
        var emails = mailcatcherContainer.getAllEmails();
        assertThat(emails).hasSizeGreaterThanOrEqualTo(1).anySatisfy(email -> {
            assertThat(email.getRecipients()).contains("<barry.rodriquez@zun.mm>");
            assertThat(email.getSubject()).isEqualTo("Registration Confirmed");
            assertThat(email.getPlainTextBody()).contains("Thank you!");
        });

        // Verify registered_at timestamp is set
        var registrationEmail = registrationEmailRepository.findById(registrationEmailId).orElseThrow();
        assertThat(registrationEmail.getRegisteredAt()).isNotNull();
    }

    @Test
    void register_second_time_sends_update_confirmation_email() {
        // Given: Registration email ID 4 - register first time
        var registrationEmailId = 4L;
        var registrationId = 3L;
        var personId = 2L;
        var event4Id = 4L;
        var event5Id = 5L;

        var initialRegistrations = Set.of(createEventRegistration(registrationId, event4Id, personId, true),
                createEventRegistration(registrationId, event5Id, personId, false));

        // First registration
        registrationService.register(registrationEmailId, initialRegistrations);
        var emailsAfterFirst = mailcatcherContainer.getAllEmails();
        var firstEmailCount = emailsAfterFirst.size();

        // When: Update registration (second time)
        var updatedRegistrations = Set.of(createEventRegistration(registrationId, event4Id, personId, false),
                createEventRegistration(registrationId, event5Id, personId, true));

        registrationService.register(registrationEmailId, updatedRegistrations);

        // Then: Update confirmation email should be sent
        var allEmails = mailcatcherContainer.getAllEmails();
        assertThat(allEmails).hasSizeGreaterThan(firstEmailCount);

        // Find the update confirmation email (should be the most recent one)
        var updateEmail = allEmails.get(allEmails.size() - 1);
        assertThat(updateEmail.getSubject()).isEqualTo("Registration Updated");
        assertThat(updateEmail.getPlainTextBody()).contains("Updated!");

        // Verify event registrations were updated
        var event4Registration = eventRegistrationRepository
            .findByRegistrationIdAndEventIdAndPersonId(registrationId, event4Id, personId)
            .orElseThrow();
        var event5Registration = eventRegistrationRepository
            .findByRegistrationIdAndEventIdAndPersonId(registrationId, event5Id, personId)
            .orElseThrow();

        assertThat(event4Registration.getRegistered()).isFalse();
        assertThat(event5Registration.getRegistered()).isTrue();
    }

    @Test
    void register_with_detailed_template_replaces_all_placeholders() {
        // Given: Registration email ID 2 with person 5 (Cora Tesi)
        // Using registration 1 which has detailed confirmation templates
        // First, let's create a registration email for registration 1
        var registrationEmailId = 2L; // This is for registration 1, person 5
        var registrationId = 1L;
        var personId = 5L; // Cora Tesi
        var event1Id = 1L; // CIS 2023
        var event2Id = 2L; // Jugendmeisterschaft 2023

        var eventRegistrations = Set.of(createEventRegistration(registrationId, event1Id, personId, true),
                createEventRegistration(registrationId, event2Id, personId, false));

        // When: Register
        registrationService.register(registrationEmailId, eventRegistrations);

        // Then: Check email with all placeholders replaced
        var emails = mailcatcherContainer.getAllEmails();
        assertThat(emails).hasSizeGreaterThanOrEqualTo(1).anySatisfy(email -> {
            assertThat(email.getSubject()).isEqualTo("Registration Confirmed");
            var body = email.getPlainTextBody();

            // Check all placeholders are replaced
            assertThat(body).contains("Cora Tesi"); // %PERSON_NAMES%
            assertThat(body).contains("CIS 2023"); // %EVENTS%
            assertThat(body).contains("Jugendmeisterschaft 2023"); // %EVENTS%
            assertThat(body).contains("Ja"); // Registered for event 1
            assertThat(body).contains("Nein"); // Not registered for event 2
            assertThat(body).contains("01.01.2023"); // %OPEN_FROM%
            assertThat(body).contains("28.02.2023"); // %OPEN_UNTIL%
            assertThat(body).contains("Some remarks"); // %REMARKS%
            assertThat(body).contains("https://anmeldungen.tverlach.ch/public/2226914588a24213a631dcdd475f81b6"); // %LINK%

            // Verify no placeholders remain
            assertThat(body).doesNotContain("%PERSON_NAMES%")
                .doesNotContain("%EVENTS%")
                .doesNotContain("%LINK%")
                .doesNotContain("%OPEN_FROM%")
                .doesNotContain("%OPEN_UNTIL%")
                .doesNotContain("%REMARKS%");
        });
    }

    @Test
    void register_without_event_registrations_does_not_send_email() {
        // Given: Empty event registrations set
        var registrationEmailId = 5L;
        var eventRegistrations = Set.<EventRegistrationRecord>of();

        var emailsBeforeRegister = mailcatcherContainer.getAllEmails().size();

        // When: Register with empty set
        registrationService.register(registrationEmailId, eventRegistrations);

        // Then: No email should be sent because registrations are empty
        var emailsAfterRegister = mailcatcherContainer.getAllEmails().size();
        assertThat(emailsAfterRegister).isEqualTo(emailsBeforeRegister);

        // And: registered_at should NOT be set
        var registrationEmail = registrationEmailRepository.findById(registrationEmailId).orElseThrow();
        assertThat(registrationEmail.getRegisteredAt()).isNull();
    }

    private EventRegistrationRecord createEventRegistration(Long registrationId, Long eventId, Long personId,
            boolean registered) {
        var eventRegistrationRecord = new EventRegistrationRecord();
        eventRegistrationRecord.setRegistrationId(registrationId);
        eventRegistrationRecord.setEventId(eventId);
        eventRegistrationRecord.setPersonId(personId);
        eventRegistrationRecord.setRegistered(registered);
        return eventRegistrationRecord;
    }

}
