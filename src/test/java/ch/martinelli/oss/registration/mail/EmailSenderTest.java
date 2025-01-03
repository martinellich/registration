package ch.martinelli.oss.registration.mail;

import ch.martinelli.oss.registration.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mail.SimpleMailMessage;
import skydrinker.testcontainers.mailcatcher.MailCatcherContainer;
import skydrinker.testcontainers.mailcatcher.MailcatcherMail;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class EmailSenderTest {

    @Autowired
    private EmailSender emailSender;
    @Autowired
    private MailCatcherContainer mailcatcherContainer = new MailCatcherContainer();

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
