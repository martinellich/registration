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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
public abstract class KaribuTest {

    protected static Routes routes;

    protected static String username = "test@test.ch";

    protected static String role = "APPROLE_ADMIN";

    protected static String name = "John Doe";

    @Autowired
    protected ApplicationContext ctx;

    @Autowired
    private UserDetailsService userDetailsService;

    @BeforeAll
    public static void discoverRoutes() {
        Locale.setDefault(Locale.GERMAN);
        routes = new Routes().autoDiscoverViews("ch.martinelli.oss.registration.ui.views");
    }

    @BeforeEach
    public void setup() {
        MockVaadin.INSTANCE.setMockRequestFactory(session -> new FakeRequest(session) {
            @Override
            public Principal getUserPrincipal() {
                KaribuTest.fakeLogin();
                return SecurityContextHolder.getContext().getAuthentication();
            }
        });
        final Function0<UI> uiFactory = UI::new;
        MockVaadin.setup(uiFactory, new MockSpringServlet(routes, ctx, uiFactory));
    }

    @AfterEach
    public void tearDown() {
        logout();
        MockVaadin.tearDown();
    }

    private static void fakeLogin() {
        OidcIdToken oidcIdToken = new OidcIdToken("tokenValue", null, null,
                Map.of("sub", "-", "preferred_username", username, "name", name));
        DefaultOidcUser defaultOidcUser = new DefaultOidcUser(List.of(new SimpleGrantedAuthority(role)), oidcIdToken);
        OAuth2AuthenticationToken oAuth2AuthenticationToken = new OAuth2AuthenticationToken(defaultOidcUser,
                defaultOidcUser.getAuthorities(), "oidc");

        SecurityContextHolder.getContext().setAuthentication(oAuth2AuthenticationToken);

        FakeRequest request = (FakeRequest) VaadinServletRequest.getCurrent().getRequest();
        request.setUserPrincipalInt(oAuth2AuthenticationToken);
        request.setUserInRole((principal, role) -> oAuth2AuthenticationToken.getPrincipal()
            .getAuthorities()
            .stream()
            .anyMatch(a -> a.getAuthority().equals(role)));
    }

    protected void logout() {
        try {
            SecurityContextHolder.getContext().setAuthentication(null);
            if (VaadinServletRequest.getCurrent() != null) {
                FakeRequest request = (FakeRequest) VaadinServletRequest.getCurrent().getRequest();
                request.setUserPrincipalInt(null);
                request.setUserInRole((principal, role) -> false);
            }
        }
        catch (IllegalStateException e) {
            // Ignored
        }
    }

}
