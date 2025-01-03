package ch.martinelli.oss.registration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import skydrinker.testcontainers.mailcatcher.MailCatcherContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.1"));
    }

    @Bean
    MailCatcherContainer mailcatcherContainer() {
        MailCatcherContainer mailCatcherContainer = new MailCatcherContainer();
        mailCatcherContainer.start();
        return mailCatcherContainer;
    }

    @Bean
    JavaMailSender mailSender(MailCatcherContainer mailcatcherContainer) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(mailcatcherContainer.getHost());
        sender.setPort(mailcatcherContainer.getSmtpPort());
        return sender;
    }
}