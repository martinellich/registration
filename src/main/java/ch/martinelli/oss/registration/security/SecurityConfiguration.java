package ch.martinelli.oss.registration.security;

import com.azure.spring.cloud.autoconfigure.implementation.aad.security.AadWebApplicationHttpSecurityConfigurer;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@EnableWebSecurity
@Configuration
public class SecurityConfiguration extends VaadinWebSecurity {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.with(AadWebApplicationHttpSecurityConfigurer.aadWebApplication(), c -> {
        });

        http.authorizeHttpRequests(
                authorize -> authorize.requestMatchers("/images/*.png", "/line-awesome/*", "/oauth/token")
                    .permitAll()
                    .requestMatchers(EndpointRequest.to(HealthEndpoint.class))
                    .permitAll());

        super.configure(http);
    }

}
