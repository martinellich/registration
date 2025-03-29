package ch.martinelli.oss.registration.ui.views;

import ch.martinelli.oss.registration.TestcontainersConfiguration;
import ch.martinelli.oss.registration.security.Roles;
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

    private static Routes routes;

    private String username = "john.doe@test.com";

    private String name = "John Doe";

    private String role = Roles.ADMIN;

    @Autowired
    protected ApplicationContext ctx;

    private OAuth2AuthenticationToken oAuth2AuthenticationToken;

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
                createAuthentication();
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

    protected void login(String username, String role) {
        this.username = username;
        this.role = role;
        oAuth2AuthenticationToken = null;
        createOAuth2AuthenticationToken();
    }

    private void createAuthentication() {
        createOAuth2AuthenticationToken();
        SecurityContextHolder.getContext().setAuthentication(oAuth2AuthenticationToken);

        var request = (FakeRequest) VaadinServletRequest.getCurrent().getRequest();
        request.setUserPrincipalInt(oAuth2AuthenticationToken);
        request.setUserInRole((principal, roleName) -> oAuth2AuthenticationToken.getPrincipal()
            .getAuthorities()
            .stream()
            .anyMatch(a -> a.getAuthority().equals(roleName)));
    }

    private void createOAuth2AuthenticationToken() {
        if (oAuth2AuthenticationToken == null) {
            var oidcIdToken = new OidcIdToken("tokenValue", null, null,
                    Map.of("sub", "-", "preferred_username", username, "name", name));
            var defaultOidcUser = new DefaultOidcUser(List.of(new SimpleGrantedAuthority(role)), oidcIdToken);
            oAuth2AuthenticationToken = new OAuth2AuthenticationToken(defaultOidcUser, defaultOidcUser.getAuthorities(),
                    "oidc");
        }
    }

    protected void logout() {
        try {
            SecurityContextHolder.getContext().setAuthentication(null);
            if (VaadinServletRequest.getCurrent() != null) {
                var request = (FakeRequest) VaadinServletRequest.getCurrent().getRequest();
                request.setUserPrincipalInt(null);
                request.setUserInRole((principal, roleName) -> false);
            }
        }
        catch (IllegalStateException e) {
            // Ignored
        }
    }

}
