package ch.martinelli.oss.registration.mail;

import ch.martinelli.oss.registration.db.tables.records.RegistrationEmailViewRecord;
import ch.martinelli.oss.registration.db.tables.records.RegistrationRecord;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static ch.martinelli.oss.registration.db.tables.RegistrationEmail.REGISTRATION_EMAIL;

@Component
public class EmailSender {

    private final JavaMailSender javaMailSender;
    private final DSLContext dslContext;
    private final String publicAddress;
    private final String sender;

    public EmailSender(JavaMailSender javaMailSender, DSLContext dslContext,
                       @Value("${public.address}") String publicAddress,
                       @Value("${spring.mail.username}") String sender) {
        this.javaMailSender = javaMailSender;
        this.dslContext = dslContext;
        this.publicAddress = publicAddress;
        this.sender = sender;
    }

    @Transactional
    public void sendEmail(RegistrationRecord registration, RegistrationEmailViewRecord registrationEmail) {
        SimpleMailMessage mailMessage = createMailMessage(registration, registrationEmail);

        try {
            javaMailSender.send(mailMessage);
        } catch (Exception e) {
            throw new MailSendException(
                    "Failed to send email with RegistrationEmailId %d".formatted(registrationEmail.getRegistrationEmailId()),
                    e);
        }

        dslContext.update(REGISTRATION_EMAIL)
                .set(REGISTRATION_EMAIL.SENT_AT, LocalDateTime.now())
                .where(REGISTRATION_EMAIL.ID.eq(registrationEmail.getRegistrationEmailId()))
                .execute();
    }

    private SimpleMailMessage createMailMessage(RegistrationRecord registration, RegistrationEmailViewRecord registrationEmail) {
        if (registration.getEmailText() == null) {
            throw new IllegalArgumentException("Email text is missing for registration %d".formatted(registration.getId()));
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(registrationEmail.getEmail());
        message.setSubject("%s %d".formatted(registration.getTitle(), registration.getYear()));
        String url = "%s/public/%s".formatted(publicAddress, registrationEmail.getLink());
        message.setText(registration.getEmailText().formatted(url));
        return message;
    }
}
