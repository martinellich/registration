package ch.martinelli.oss.registration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RegistrationApplicationTest {

    @Test
    void contextLoads() {
        // Test if the context loads
    }

}
