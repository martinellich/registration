package ch.martinelli.oss.registration.security;

import com.azure.spring.cloud.autoconfigure.implementation.aad.security.AadWebApplicationHttpSecurityConfigurer;
import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategyConfiguration;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@Import(VaadinAwareSecurityContextHolderStrategyConfiguration.class)
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.with(AadWebApplicationHttpSecurityConfigurer.aadWebApplication(), c -> {
        });

        http.authorizeHttpRequests(
                c -> c.requestMatchers("/images/*.png", "/line-awesome/*", "oauth2/authorization/azure")
                    .permitAll()
                    .requestMatchers(EndpointRequest.to(HealthEndpoint.class))
                    .permitAll());

        return http.with(VaadinSecurityConfigurer.vaadin(), configurer -> {
        }).build();
    }

}
