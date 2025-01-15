package ch.martinelli.oss.registration.mail;

import ch.martinelli.oss.registration.TestcontainersConfiguration;
import ch.martinelli.oss.registration.db.tables.records.RegistrationEmailViewRecord;
import ch.martinelli.oss.registration.db.tables.records.RegistrationRecord;
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
import skydrinker.testcontainers.mailcatcher.MailcatcherMail;

import java.util.List;

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
        RegistrationRecord registration = registrationRepository.findById(1L).orElseThrow();
        RegistrationEmailViewRecord registrationEmail = registrationEmailRepository.findByIdFromView(2L).orElseThrow();

        emailSender.sendEmail(registration, registrationEmail, "jugi@tverlach.ch");

        List<MailcatcherMail> emails = mailcatcherContainer.getAllEmails();

        assertThat(emails).hasSize(1).first().satisfies(email -> {
            assertThat(email.getSender()).isEqualTo("<jugi@tverlach.ch>");
            assertThat(email.getSubject()).isEqualTo("Anmeldung 2023");
            assertThat(email.getPlainTextBody())
                .isEqualTo("Mail text https://tve-registration.fly.dev/public/2226914588a24213a631dcdd475f81b6\n");
        });
    }

}
