package ch.martinelli.oss.registration.security;

import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Takes care of all such static operations that have to do with security and
 * querying rights from different beans of the UI.
 */
@Component
public final class SecurityContext {

    private final AuthenticationContext authenticationContext;

    public SecurityContext(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
    }

    /**
     * Gets the username of the currently signed-in user.
     *
     * @return the username of the current user or <code>null</code> if the user
     * has not signed in
     */
    public String getUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return switch (principal) {
            case UserDetails userDetails -> userDetails.getUsername();
            case Jwt jwt -> jwt.getSubject();
            case null, default -> ""; // Anonymous or no authentication.
        };
    }

    /**
     * Checks if the user is logged in.
     *
     * @return true if the user is logged in. False otherwise.
     */
    public boolean isUserLoggedIn() {
        return isUserLoggedIn(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Determines whether a user is currently logged in based on the provided authentication object.
     *
     * @param authentication the authentication object representing the user's current authentication state
     * @return true if the user is authenticated and not anonymous, false otherwise
     */
    private boolean isUserLoggedIn(Authentication authentication) {
        return authentication != null && !(authentication instanceof AnonymousAuthenticationToken);
    }

    /**
     * Logs the currently authenticated user out of the application.
     * <p>
     * This method handles the following operations during logout:
     * 1. Retrieve the current HTTP request and log the user out via the `AuthenticationContext`.
     * 2. Invalidate the "remember-me" cookie by setting its value to `null`
     * and max age to `0`, effectively clearing it from the client.
     * 3. Adjust the cookie path based on the application context path.
     * 4. Add the invalidated cookie to the HTTP response to ensure it is removed on the client-side.
     */
    public void logout() {
        HttpServletRequest request = VaadinServletRequest.getCurrent().getHttpServletRequest();

        authenticationContext.logout();

        Cookie cookie = new Cookie("remember-me", null);
        cookie.setMaxAge(0);
        cookie.setPath(StringUtils.hasLength(request.getContextPath()) ? request.getContextPath() : "/");

        HttpServletResponse response = (HttpServletResponse) VaadinResponse.getCurrent();
        response.addCookie(cookie);
    }
}
