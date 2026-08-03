package ch.martinelli.oss.registration.mail;

import ch.martinelli.oss.registration.TestcontainersConfiguration;
import ch.martinelli.oss.registration.domain.EmailSender;
import ch.martinelli.oss.registration.domain.RegistrationEmailRepository;
import ch.martinelli.oss.registration.domain.RegistrationRepository;
import ch.martinelli.oss.testcontainers.mailpit.MailpitContainer;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static ch.martinelli.oss.registration.db.tables.RegistrationEmail.REGISTRATION_EMAIL;
import static ch.martinelli.oss.registration.db.tables.RegistrationEmailPerson.REGISTRATION_EMAIL_PERSON;
import static ch.martinelli.oss.testcontainers.mailpit.assertions.MailpitAssertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class EmailSenderTest {

    @Autowired
    private MailpitContainer mailpitContainer;

    @Autowired
    private EmailSender emailSender;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private RegistrationEmailRepository registrationEmailRepository;

    @Autowired
    private DSLContext dslContext;

    @BeforeEach
    void setUp() {
        // Clear email container before each test
        mailpitContainer.getClient().deleteAllMessages();

        // Ensure registration_email ID 2 exists (may be deleted by other tests)
        var exists = dslContext
            .fetchExists(dslContext.selectFrom(REGISTRATION_EMAIL).where(REGISTRATION_EMAIL.ID.eq(2L)));
        if (!exists) {
            // Re-insert registration_email ID 2 and its person association
            dslContext
                .insertInto(REGISTRATION_EMAIL, REGISTRATION_EMAIL.ID, REGISTRATION_EMAIL.REGISTRATION_ID,
                        REGISTRATION_EMAIL.EMAIL, REGISTRATION_EMAIL.LINK)
                .values(2L, 1L, "cora.tesi@bivo.yt", "2226914588a24213a631dcdd475f81b6")
                .execute();
            dslContext
                .insertInto(REGISTRATION_EMAIL_PERSON, REGISTRATION_EMAIL_PERSON.REGISTRATION_EMAIL_ID,
                        REGISTRATION_EMAIL_PERSON.PERSON_ID)
                .values(2L, 5L)
                .execute();
        }
        else {
            // Reset registered_at in case another test modified it
            dslContext.update(REGISTRATION_EMAIL)
                .setNull(REGISTRATION_EMAIL.REGISTERED_AT)
                .setNull(REGISTRATION_EMAIL.SENT_AT)
                .where(REGISTRATION_EMAIL.ID.eq(2L))
                .execute();
        }
    }

    @Test
    void send_mails() {
        var registration = registrationRepository.findById(1L).orElseThrow();
        var registrationEmail = registrationEmailRepository.findByIdFromView(2L).orElseThrow();

        emailSender.sendEmail(registration, registrationEmail, "jugi@tverlach.ch");

        // Check that our invitation email is in there
        assertThat(mailpitContainer).hasMessages()
            .firstMessage()
            .isFrom("jugi@tverlach.ch")
            .hasSubject("Anmeldung 2023")
            .hasSnippetContaining("Mail text https://anmeldungen.tverlach.ch/public/2226914588a24213a631dcdd475f81b6");
    }

    @Test
    void send_confirmation_email() {
        emailSender.sendConfirmationEmail("test@example.com", "Anmeldebestätigung",
                "Vielen Dank für deine Anmeldung!\n\nDeine Anlässe:\n- Event 1: Ja\n- Event 2: Nein",
                "jugi@tverlach.ch");

        assertThat(mailpitContainer).hasMessages()
            .firstMessage()
            .isFrom("jugi@tverlach.ch")
            .hasRecipient("test@example.com")
            .hasSubject("Anmeldebestätigung")
            .hasSnippetContaining("Vielen Dank für deine Anmeldung!")
            .hasSnippetContaining("Event 1: Ja")
            .hasSnippetContaining("Event 2: Nein");
    }

}
