package ch.martinelli.oss.registration.mail;

import ch.martinelli.oss.registration.TestcontainersConfiguration;
import ch.martinelli.oss.registration.domain.EmailSender;
import ch.martinelli.oss.registration.domain.RegistrationEmailRepository;
import ch.martinelli.oss.registration.domain.RegistrationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import skydrinker.testcontainers.mailcatcher.MailCatcherContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class EmailSenderTest {

    @Container
    static final MailCatcherContainer mailcatcherContainer = new MailCatcherContainer();

    @Autowired
    private EmailSender emailSender;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private RegistrationEmailRepository registrationEmailRepository;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        mailcatcherContainer.start();

        registry.add("spring.mail.host", mailcatcherContainer::getHost);
        registry.add("spring.mail.port", mailcatcherContainer::getSmtpPort);
        registry.add("spring.mail.username", () -> "jugi@tverlach.ch");
        registry.add("spring.mail.password", () -> "pass");
    }

    @Test
    void send_mails() {
        var registration = registrationRepository.findById(1L).orElseThrow();
        var registrationEmail = registrationEmailRepository.findByIdFromView(2L).orElseThrow();

        emailSender.sendEmail(registration, registrationEmail, "jugi@tverlach.ch");

        var emails = mailcatcherContainer.getAllEmails();

        // Check that our invitation email is in there
        assertThat(emails).hasSizeGreaterThanOrEqualTo(1).anySatisfy(email -> {
            assertThat(email.getSender()).isEqualTo("<jugi@tverlach.ch>");
            assertThat(email.getSubject()).isEqualTo("Anmeldung 2023");
            assertThat(email.getPlainTextBody())
                .isEqualTo("Mail text https://anmeldungen.tverlach.ch/public/2226914588a24213a631dcdd475f81b6\n");
        });
    }

    @Test
    void send_confirmation_email() {
        emailSender.sendConfirmationEmail("test@example.com", "Anmeldebestätigung",
                "Vielen Dank für deine Anmeldung!\n\nDeine Anlässe:\n- Event 1: Ja\n- Event 2: Nein",
                "jugi@tverlach.ch");

        var emails = mailcatcherContainer.getAllEmails();

        // Should have 2 emails now (1 from previous test + 1 from this test)
        assertThat(emails).hasSizeGreaterThanOrEqualTo(1).anySatisfy(email -> {
            assertThat(email.getSender()).isEqualTo("<jugi@tverlach.ch>");
            assertThat(email.getRecipients()).contains("<test@example.com>");
            assertThat(email.getSubject()).isEqualTo("Anmeldebestätigung");
            assertThat(email.getPlainTextBody()).contains("Vielen Dank für deine Anmeldung!")
                .contains("Event 1: Ja")
                .contains("Event 2: Nein");
        });
    }

}
