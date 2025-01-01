package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.registration.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class EventRegistrationRepositoryTest {

    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;

    @Test
    void getEventRegistrationMatrix() {
        List<EventRegistrationRow> eventRegistrationMatrix = eventRegistrationRepository.getEventRegistrationMatrix(1L);

        assertThat(eventRegistrationMatrix).isNotEmpty()
                .first()
                .satisfies(eventRegistrationRow -> {
                    assertThat(eventRegistrationRow.lastName()).isEqualTo("Lane");
                    assertThat(eventRegistrationRow.firstName()).isEqualTo("Eula");
                    assertThat(eventRegistrationRow.registrations()).hasSize(1);
                });
    }

}