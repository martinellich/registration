package ch.martinelli.oss.registration.mail;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class EmailSender {

    private final JavaMailSender javaMailSender;

    public EmailSender(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    public void send(Set<SimpleMailMessage> mails) {
        mails.forEach(javaMailSender::send);
    }
}
