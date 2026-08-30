package ch.martinelli.oss.registration.mail;

import ch.martinelli.oss.registration.TestcontainersConfiguration;
import ch.martinelli.oss.registration.domain.EmailSender;
import ch.martinelli.oss.registration.domain.RegistrationEmailRepository;
import ch.martinelli.oss.registration.domain.RegistrationRepository;
import ch.martinelli.oss.testcontainers.mailpit.MailpitContainer;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static ch.martinelli.oss.registration.db.tables.RegistrationEmail.REGISTRATION_EMAIL;
import static ch.martinelli.oss.registration.db.tables.RegistrationEmailPerson.REGISTRATION_EMAIL_PERSON;
import static ch.martinelli.oss.testcontainers.mailpit.assertions.MailpitAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The test system runs with registration.mail.enabled=false: nothing may be sent, but the
 * invitation is still marked as sent so the registration workflow can be tested.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "registration.mail.enabled=false")
class EmailSenderDisabledTest {

    @Autowired
    private MailpitContainer mailpitContainer;

    @Autowired
    private EmailSender emailSender;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private RegistrationEmailRepository registrationEmailRepository;

    @Autowired
    private DSLContext dslContext;

    @BeforeEach
    void setUp() {
        mailpitContainer.getClient().deleteAllMessages();

        // Ensure registration_email ID 2 exists (may be deleted by other tests)
        var exists = dslContext
            .fetchExists(dslContext.selectFrom(REGISTRATION_EMAIL).where(REGISTRATION_EMAIL.ID.eq(2L)));
        if (!exists) {
            dslContext
                .insertInto(REGISTRATION_EMAIL, REGISTRATION_EMAIL.ID, REGISTRATION_EMAIL.REGISTRATION_ID,
                        REGISTRATION_EMAIL.EMAIL, REGISTRATION_EMAIL.LINK)
                .values(2L, 1L, "cora.tesi@bivo.yt", "2226914588a24213a631dcdd475f81b6")
                .execute();
            dslContext
                .insertInto(REGISTRATION_EMAIL_PERSON, REGISTRATION_EMAIL_PERSON.REGISTRATION_EMAIL_ID,
                        REGISTRATION_EMAIL_PERSON.PERSON_ID)
                .values(2L, 5L)
                .execute();
        }
        else {
            dslContext.update(REGISTRATION_EMAIL)
                .setNull(REGISTRATION_EMAIL.REGISTERED_AT)
                .setNull(REGISTRATION_EMAIL.SENT_AT)
                .where(REGISTRATION_EMAIL.ID.eq(2L))
                .execute();
        }
    }

    @Test
    void invitation_is_marked_as_sent_but_not_delivered() {
        var registration = registrationRepository.findById(1L).orElseThrow();
        var registrationEmail = registrationEmailRepository.findByIdFromView(2L).orElseThrow();

        emailSender.sendEmail(registration, registrationEmail, "jugi@tverlach.ch");

        assertThat(mailpitContainer).hasNoMessages();
        var sentAt = dslContext.select(REGISTRATION_EMAIL.SENT_AT)
            .from(REGISTRATION_EMAIL)
            .where(REGISTRATION_EMAIL.ID.eq(2L))
            .fetchOne(REGISTRATION_EMAIL.SENT_AT);
        assertThat(sentAt).isNotNull();
    }

    @Test
    void confirmation_is_not_delivered() {
        emailSender.sendConfirmationEmail("test@example.com", "Anmeldebestätigung", "Vielen Dank!", "jugi@tverlach.ch");

        assertThat(mailpitContainer).hasNoMessages();
    }

}
