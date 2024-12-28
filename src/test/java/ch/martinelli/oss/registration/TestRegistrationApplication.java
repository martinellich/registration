package ch.martinelli.oss.registration;

import org.springframework.boot.SpringApplication;

public class TestRegistrationApplication {

    public static void main(String[] args) {
            SpringApplication.from(RegistrationApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
