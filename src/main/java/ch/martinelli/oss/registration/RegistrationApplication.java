package ch.martinelli.oss.registration;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Theme(value = "registrations")
public class RegistrationApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(RegistrationApplication.class, args);
    }

}
