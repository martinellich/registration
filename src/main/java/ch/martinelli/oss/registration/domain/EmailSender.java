package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.registration.db.tables.records.RegistrationEmailViewRecord;
import ch.martinelli.oss.registration.db.tables.records.RegistrationRecord;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final JavaMailSender javaMailSender;

    private final DSLContext dslContext;

    private final String publicAddress;

    private final String sender;

    private final boolean mailEnabled;

    public EmailSender(JavaMailSender javaMailSender, DSLContext dslContext,
            @Value("${registration.public.address}") String publicAddress,
            @Value("${spring.mail.username:jugi@tverlach.ch}") String sender,
            @Value("${registration.mail.enabled}") boolean mailEnabled) {
        this.javaMailSender = javaMailSender;
        this.dslContext = dslContext;
        this.publicAddress = publicAddress;
        this.sender = sender;
        this.mailEnabled = mailEnabled;
    }

    @Transactional
    public void sendEmail(RegistrationRecord registration, RegistrationEmailViewRecord registrationEmail,
            String replyTo) {
        try {
            var mailMessage = createMailMessage(registration, registrationEmail, replyTo);
            send(mailMessage);

            log.info("Email sent to {} with RegistrationEmailId {}", registrationEmail.getEmail(),
                    registrationEmail.getRegistrationEmailId());
        }
        catch (Exception e) {
            throw new MailSendException("Failed to send email with RegistrationEmailId %d"
                .formatted(registrationEmail.getRegistrationEmailId()), e);
        }

        dslContext.update(REGISTRATION_EMAIL)
            .set(REGISTRATION_EMAIL.SENT_AT, LocalDateTime.now())
            .where(REGISTRATION_EMAIL.ID.eq(registrationEmail.getRegistrationEmailId()))
            .execute();
    }

    public void sendConfirmationEmail(String to, String subject, String body, String replyTo) {
        try {
            var message = new SimpleMailMessage();
            message.setFrom(sender);
            message.setTo(to);
            message.setReplyTo(replyTo);
            message.setSubject(subject);
            message.setText(body);
            send(message);

            log.info("Confirmation email sent to {}", to);
        }
        catch (Exception e) {
            log.error("Failed to send confirmation email to {}", to, e);
            // Don't throw exception - we don't want to block registration if email fails
        }
    }

    /**
     * On non-production systems (registration.mail.enabled=false) nothing leaves the
     * application; the message is logged instead so the workflow stays testable.
     */
    private void send(SimpleMailMessage message) {
        if (mailEnabled) {
            javaMailSender.send(message);
        }
        else {
            log.info("Mail sending is disabled, not sending: {}", message);
        }
    }

    private SimpleMailMessage createMailMessage(RegistrationRecord registration,
            RegistrationEmailViewRecord registrationEmail, String replyTo) {
        if (registration.getEmailText() == null) {
            throw new IllegalArgumentException(
                    "Email text is missing for registration %d".formatted(registration.getId()));
        }
        var message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(registrationEmail.getEmail());
        message.setReplyTo(replyTo);
        message.setSubject("%s %d".formatted(registration.getTitle(), registration.getYear()));
        var url = "%s/public/%s".formatted(publicAddress, registrationEmail.getLink());
        message.setText(registration.getEmailText().formatted(url));
        return message;
    }

}
