package ch.martinelli.oss.registration.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Component;

@Component
public final class SecurityContext {

    public String getUsername() {
        if (SecurityContextHolder.getContext().getAuthentication() != null && SecurityContextHolder.getContext()
            .getAuthentication()
            .getPrincipal() instanceof DefaultOidcUser oidcUser) {
            return oidcUser.getPreferredUsername();
        }
        else {
            return "";
        }
    }

    public String getName() {
        if (SecurityContextHolder.getContext().getAuthentication() != null && SecurityContextHolder.getContext()
            .getAuthentication()
            .getPrincipal() instanceof DefaultOidcUser oidcUser) {
            return oidcUser.getName();
        }
        else {
            return "";
        }
    }

    public boolean isUserLoggedIn() {
        return isUserLoggedIn(SecurityContextHolder.getContext().getAuthentication());
    }

    private boolean isUserLoggedIn(Authentication authentication) {
        return authentication != null && !(authentication instanceof AnonymousAuthenticationToken);
    }

}