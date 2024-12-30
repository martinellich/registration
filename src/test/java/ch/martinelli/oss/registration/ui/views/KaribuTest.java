package ch.martinelli.oss.registration.ui.views;

import ch.martinelli.oss.registration.TestcontainersConfiguration;
import com.github.mvysny.fakeservlet.FakeRequest;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.Routes;
import com.github.mvysny.kaributesting.v10.spring.MockSpringServlet;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinServletRequest;
import kotlin.jvm.functions.Function0;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.Locale;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
public abstract class KaribuTest {

    private static final String ROLE_PREFIX = "ROLE_";

    protected static Routes routes;
    protected static String packageName = "ch.martinelli.oss.registration.ui.views";

    @Autowired
    protected ApplicationContext ctx;
    @Autowired
    private UserDetailsService userDetailsService;

    @BeforeAll
    public static void discoverRoutes() {
        System.setProperty("logging.level.org.jooq", "debug");

        Locale.setDefault(Locale.GERMAN);
        routes = new Routes().autoDiscoverViews(packageName);
    }

    @BeforeEach
    public void setup() {
        final Function0<UI> uiFactory = UI::new;
        MockVaadin.setup(uiFactory, new MockSpringServlet(routes, ctx, uiFactory));
    }

    @AfterEach
    public void tearDown() {
        logout();
        MockVaadin.tearDown();
    }

    /**
     * Login with UserDetailsService
     *
     * @param username Username
     */
    protected void login(String username) {
        var userDetails = userDetailsService.loadUserByUsername(username);
        setAuthentication(userDetails);
    }

    /**
     * Login with fake-user
     *
     * @param username Username
     * @param roles    List of roles (without ROLE_ prefix)
     */
    protected void login(String username, final List<String> roles) {
        // taken from https://www.baeldung.com/manually-set-user-authentication-spring-security
        // also see https://github.com/mvysny/karibu-testing/issues/47 for more details.
        var authorities = roles.stream()
                .map(it -> new SimpleGrantedAuthority(ROLE_PREFIX + it))
                .toList();

        var userDetails = new User(username, "pass", authorities);
        setAuthentication(userDetails);
    }

    private static void setAuthentication(UserDetails userDetails) {
        var authReq = new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword(),
                userDetails.getAuthorities());
        var sc = SecurityContextHolder.getContext();
        sc.setAuthentication(authReq);

        final var request = (FakeRequest) VaadinServletRequest.getCurrent().getRequest();
        request.setUserPrincipalInt(authReq);
        request.setUserInRole((principal, role) -> userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role) || a.getAuthority().equals(ROLE_PREFIX + role)));
    }

    protected void logout() {
        try {
            SecurityContextHolder.getContext().setAuthentication(null);
            if (VaadinServletRequest.getCurrent() != null) {
                final FakeRequest request = (FakeRequest) VaadinServletRequest.getCurrent().getRequest();
                request.setUserPrincipalInt(null);
                request.setUserInRole((principal, role) -> false);
            }
        } catch (IllegalStateException e) {
            // Ignored
        }
    }

}
