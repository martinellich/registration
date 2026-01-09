package ch.martinelli.oss.registration.security;

import com.azure.spring.cloud.autoconfigure.implementation.aad.security.AadWebApplicationHttpSecurityConfigurer;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http.with(AadWebApplicationHttpSecurityConfigurer.aadWebApplication(), c -> {
        });

        http.authorizeHttpRequests(c -> c
            .requestMatchers("/*.css", "/*/*.css", "/icons/*.*", "/images/*.*", "/line-awesome/*/*.*",
                    "/oauth2/authorization/azure")
            .permitAll()
            .requestMatchers(EndpointRequest.to(HealthEndpoint.class))
            .permitAll());

        return http.with(VaadinSecurityConfigurer.vaadin(), _ -> {
        }).build();
    }

}
