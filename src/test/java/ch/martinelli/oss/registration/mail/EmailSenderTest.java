package ch.martinelli.oss.registration.mail;

import ch.martinelli.oss.registration.TestcontainersConfiguration;
import ch.martinelli.oss.registration.domain.EmailSender;
import ch.martinelli.oss.registration.domain.RegistrationEmailRepository;
import ch.martinelli.oss.registration.domain.RegistrationRepository;
import ch.martinelli.oss.testcontainers.mailpit.Address;
import ch.martinelli.oss.testcontainers.mailpit.MailpitClient;
import ch.martinelli.oss.testcontainers.mailpit.MailpitContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class EmailSenderTest {

    @Container
    static final MailpitContainer mailpitContainer = new MailpitContainer();

    @Autowired
    private EmailSender emailSender;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private RegistrationEmailRepository registrationEmailRepository;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        mailpitContainer.start();

        registry.add("spring.mail.host", mailpitContainer::getHost);
        registry.add("spring.mail.port", mailpitContainer::getSmtpPort);
        registry.add("spring.mail.username", () -> "jugi@tverlach.ch");
        registry.add("spring.mail.password", () -> "pass");
    }

    @Test
    void send_mails() {
        var registration = registrationRepository.findById(1L).orElseThrow();
        var registrationEmail = registrationEmailRepository.findByIdFromView(2L).orElseThrow();

        emailSender.sendEmail(registration, registrationEmail, "jugi@tverlach.ch");

        MailpitClient client = mailpitContainer.getClient();
        var messages = client.getAllMessages();

        // Check that our invitation email is in there
        assertThat(messages).hasSizeGreaterThanOrEqualTo(1).anySatisfy(message -> {
            assertThat(message.from().address()).isEqualTo("jugi@tverlach.ch");
            assertThat(message.subject()).isEqualTo("Anmeldung 2023");
            assertThat(client.getMessagePlain(message.id()))
                .contains("Mail text https://anmeldungen.tverlach.ch/public/2226914588a24213a631dcdd475f81b6");
        });
    }

    @Test
    void send_confirmation_email() {
        emailSender.sendConfirmationEmail("test@example.com", "Anmeldebestätigung",
                "Vielen Dank für deine Anmeldung!\n\nDeine Anlässe:\n- Event 1: Ja\n- Event 2: Nein",
                "jugi@tverlach.ch");

        MailpitClient client = mailpitContainer.getClient();
        var messages = client.getAllMessages();

        // Should have 2 emails now (1 from previous test + 1 from this test)
        assertThat(messages).hasSizeGreaterThanOrEqualTo(1).anySatisfy(message -> {
            assertThat(message.from().address()).isEqualTo("jugi@tverlach.ch");
            assertThat(message.recipients()).extracting(Address::address).contains("test@example.com");
            assertThat(message.subject()).isEqualTo("Anmeldebestätigung");
            assertThat(client.getMessagePlain(message.id())).contains("Vielen Dank für deine Anmeldung!")
                .contains("Event 1: Ja")
                .contains("Event 2: Nein");
        });
    }

}
