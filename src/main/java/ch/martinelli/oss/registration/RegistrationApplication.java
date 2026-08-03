package ch.martinelli.oss.registration;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@StyleSheet(value = "styles.css")
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet(Lumo.UTILITY_STYLESHEET)
@EnableAsync
@SpringBootApplication
public class RegistrationApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(RegistrationApplication.class, args);
    }

}
