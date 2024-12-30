package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.registration.TestcontainersConfiguration;
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
class RegistrationServiceTest {

    @Container
    static MailCatcherContainer mailcatcherContainer = new MailCatcherContainer();

    @Autowired
    private RegistrationService registrationService;

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
        registrationService.sendMails();

        List<MailcatcherMail> emails = mailcatcherContainer.getAllEmails();

        assertThat(emails).hasSize(1).first().satisfies(email -> {
            assertThat(email.getSubject()).isEqualTo("Test Subject");
            assertThat(email.getSender()).isEqualTo("<jugi@tverlach.ch>");
        });
    }
}