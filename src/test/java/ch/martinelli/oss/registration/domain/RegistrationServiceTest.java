package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.registration.TestcontainersConfiguration;
import ch.martinelli.oss.registration.db.tables.records.EventRegistrationRecord;
import ch.martinelli.oss.testcontainers.mailpit.Address;
import ch.martinelli.oss.testcontainers.mailpit.MailpitClient;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.Set;

import static ch.martinelli.oss.registration.db.tables.EventRegistration.EVENT_REGISTRATION;
import static ch.martinelli.oss.registration.db.tables.RegistrationEmail.REGISTRATION_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RegistrationServiceTest {

    @Autowired
    private MailpitClient mailpitClient;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private RegistrationEmailRepository registrationEmailRepository;

    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;

    @Autowired
    private DSLContext dslContext;

    @BeforeEach
    void setUp() {
        // Clear email container before each test
        mailpitClient.deleteAllMessages();

        // Clean up event registrations for registration 3 (shared by multiple tests)
        // This ensures test isolation for person 2 with events 4 and 5
        dslContext.deleteFrom(EVENT_REGISTRATION)
            .where(EVENT_REGISTRATION.REGISTRATION_ID.eq(3L))
            .and(EVENT_REGISTRATION.PERSON_ID.eq(2L))
            .execute();

        // Clean up event registrations for registration 1, person 5 (used by detailed
        // template test)
        dslContext.deleteFrom(EVENT_REGISTRATION)
            .where(EVENT_REGISTRATION.REGISTRATION_ID.eq(1L))
            .and(EVENT_REGISTRATION.PERSON_ID.eq(5L))
            .execute();

        // Reset registered_at for registration_email records used in registration 3 tests
        dslContext.update(REGISTRATION_EMAIL)
            .setNull(REGISTRATION_EMAIL.REGISTERED_AT)
            .where(REGISTRATION_EMAIL.REGISTRATION_ID.eq(3L))
            .execute();

        // Reset registered_at for registration_email ID 2 (used by detailed template
        // test)
        dslContext.update(REGISTRATION_EMAIL)
            .setNull(REGISTRATION_EMAIL.REGISTERED_AT)
            .where(REGISTRATION_EMAIL.ID.eq(2L))
            .execute();
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
        var messages = mailpitClient.getAllMessages();
        assertThat(messages).hasSizeGreaterThanOrEqualTo(1).anySatisfy(message -> {
            assertThat(message.recipients()).extracting(Address::address).contains("barry.rodriquez@zun.mm");
            assertThat(message.subject()).isEqualTo("Registration Confirmed");
            assertThat(mailpitClient.getMessagePlain(message.id())).contains("Thank you!");
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

        // Verify registered_at is set after first registration
        var registrationEmailAfterFirst = registrationEmailRepository.findById(registrationEmailId).orElseThrow();
        assertThat(registrationEmailAfterFirst.getRegisteredAt()).isNotNull();

        var messagesAfterFirst = mailpitClient.getAllMessages();
        var firstMessageCount = messagesAfterFirst.size();

        // When: Update registration (second time)
        var updatedRegistrations = Set.of(createEventRegistration(registrationId, event4Id, personId, false),
                createEventRegistration(registrationId, event5Id, personId, true));

        registrationService.register(registrationEmailId, updatedRegistrations);

        // Then: Update confirmation email should be sent
        var allMessages = mailpitClient.getAllMessages();
        assertThat(allMessages).hasSizeGreaterThan(firstMessageCount)

            // Find the update confirmation email by subject (don't rely on message order)
            .anySatisfy(message -> {
                assertThat(message.subject()).isEqualTo("Registration Updated");
                assertThat(mailpitClient.getMessagePlain(message.id())).contains("Updated!");
            });

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
        var messages = mailpitClient.getAllMessages();
        assertThat(messages).hasSizeGreaterThanOrEqualTo(1).anySatisfy(message -> {
            assertThat(message.subject()).isEqualTo("Registration Confirmed");
            var body = mailpitClient.getMessagePlain(message.id());

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

        var messagesBeforeRegister = mailpitClient.getAllMessages().size();

        // When: Register with empty set
        registrationService.register(registrationEmailId, eventRegistrations);

        // Then: No email should be sent because registrations are empty
        var messagesAfterRegister = mailpitClient.getAllMessages().size();
        assertThat(messagesAfterRegister).isEqualTo(messagesBeforeRegister);

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
