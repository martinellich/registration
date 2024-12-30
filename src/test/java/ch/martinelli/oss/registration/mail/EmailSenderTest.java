package ch.martinelli.oss.registration.mail;

import ch.martinelli.oss.registration.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import skydrinker.testcontainers.mailcatcher.MailCatcherContainer;
import skydrinker.testcontainers.mailcatcher.MailcatcherMail;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class EmailSenderTest {

    @Container
    static MailCatcherContainer mailcatcherContainer = new MailCatcherContainer();

    @Autowired
    private EmailSender emailSender;

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
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom("jugi@tverlach.ch");
        mail.setTo("john@doe.com");
        mail.setSubject("Test Subject");
        mail.setText("Test Text");

        emailSender.send(Set.of(mail));

        List<MailcatcherMail> emails = mailcatcherContainer.getAllEmails();

        assertThat(emails).hasSize(1).first().satisfies(email -> {
            assertThat(email.getSender()).isEqualTo("<jugi@tverlach.ch>");
            assertThat(email.getSubject()).isEqualTo("Test Subject");
            assertThat(email.getPlainTextBody()).isEqualTo("Test Text\n");
        });
    }
}
