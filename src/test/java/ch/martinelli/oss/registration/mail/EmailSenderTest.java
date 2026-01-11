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

import java.time.Duration;

import static ch.martinelli.oss.registration.db.tables.RegistrationEmail.REGISTRATION_EMAIL;
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
        // Clear email container before each test and verify it's empty
        mailpitContainer.getClient().deleteAllMessages();
        // Wait for container to confirm messages are deleted
        assertThat(mailpitContainer).withTimeout(Duration.ofSeconds(5)).hasMessageCount(0);

        // Reset sent_at for registration_email ID 1 (used by send_mails test)
        dslContext.update(REGISTRATION_EMAIL)
            .setNull(REGISTRATION_EMAIL.SENT_AT)
            .where(REGISTRATION_EMAIL.ID.eq(1L))
            .execute();
    }

    @Test
    void send_mails() {
        var registration = registrationRepository.findById(1L).orElseThrow();
        var registrationEmail = registrationEmailRepository.findByIdFromView(1L).orElseThrow();

        emailSender.sendEmail(registration, registrationEmail, "jugi@tverlach.ch");

        // Check that our invitation email is in there
        assertThat(mailpitContainer).hasMessages()
            .firstMessage()
            .isFrom("jugi@tverlach.ch")
            .hasSubject("Anmeldung 2023")
            .hasSnippetContaining("Mail text https://anmeldungen.tverlach.ch/public/550e8400e29b41d4a716446655440000");
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
