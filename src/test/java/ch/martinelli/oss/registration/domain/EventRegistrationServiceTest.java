package ch.martinelli.oss.registration.domain;

import ch.martinelli.oss.registration.ui.views.KaribuTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class EventRegistrationServiceTest extends KaribuTest {

    @Autowired
    private EventRegistrationService eventRegistrationService;

    @Test
    void createEventRegistrationExcel() {
        var excelFile = eventRegistrationService.createEventRegistrationExcel(1L);

        assertThat(excelFile).isNotEmpty();
    }

}