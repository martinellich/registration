package ch.martinelli.oss.registration.ui.views;

import ch.martinelli.oss.registration.security.SecurityContext;
import ch.martinelli.oss.registration.ui.views.events.EventsView;
import ch.martinelli.oss.registration.ui.views.persons.PersonsView;
import ch.martinelli.oss.registration.ui.views.registration.EventRegistrationView;
import ch.martinelli.oss.registration.ui.views.registration.RegistrationEmailView;
import ch.martinelli.oss.registration.ui.views.registration.RegistrationView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.beans.factory.annotation.Value;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.util.Locale;

import static com.vaadin.flow.i18n.I18NProvider.translate;

/**
 * The main view is a top-level placeholder for other views.
 */
@Layout
@AnonymousAllowed
public class MainLayout extends AppLayout {

    private final transient SecurityContext securityContext;

    private final transient AuthenticationContext authenticationContext;

    private final AccessAnnotationChecker accessAnnotationChecker;

    private final String applicationVersion;

    private H1 viewTitle;

    public MainLayout(SecurityContext securityContext, AuthenticationContext authenticationContext,
            AccessAnnotationChecker accessAnnotationChecker,
            @Value("${spring.application.version}") String applicationVersion) {
        this.securityContext = securityContext;
        this.authenticationContext = authenticationContext;
        this.accessAnnotationChecker = accessAnnotationChecker;
        this.applicationVersion = applicationVersion;

        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
    }

    private void addHeaderContent() {
        var drawerToggle = new DrawerToggle();
        drawerToggle.setAriaLabel("Menu toggle");

        viewTitle = new H1();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        addToNavbar(true, drawerToggle, viewTitle);
    }

    private void addDrawerContent() {
        var appName = new Span(translate("application.title"));
        appName.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.LARGE);
        var header = new Header(appName);

        var scroller = new Scroller(createNavigation());

        addToDrawer(header, scroller, createFooter());
    }

    private VerticalLayout createNavigation() {
        var verticalLayout = new VerticalLayout();
        verticalLayout.setPadding(false);
        verticalLayout.setSpacing(false);

        var sideNav = new SideNav();

        if (accessAnnotationChecker.hasAccess(RegistrationView.class)) {
            var invitationsNavItem = new SideNavItem(translate("invitations"), RegistrationView.class,
                    LineAwesomeIcon.LIST_SOLID.create());
            invitationsNavItem.setExpanded(true);
            sideNav.addItem(invitationsNavItem);
            if (accessAnnotationChecker.hasAccess(RegistrationEmailView.class)) {
                invitationsNavItem.addItem(new SideNavItem(translate("mailing"), RegistrationEmailView.class,
                        LineAwesomeIcon.MAIL_BULK_SOLID.create()));
            }
            if (accessAnnotationChecker.hasAccess(EventRegistrationView.class)) {
                invitationsNavItem.addItem(new SideNavItem(translate("event.registrations"),
                        EventRegistrationView.class, LineAwesomeIcon.TH_LIST_SOLID.create()));
            }
        }
        if (accessAnnotationChecker.hasAccess(EventsView.class)) {
            sideNav.addItem(
                    new SideNavItem(translate("events"), EventsView.class, LineAwesomeIcon.CALENDAR_SOLID.create()));
        }
        if (accessAnnotationChecker.hasAccess(PersonsView.class)) {
            sideNav.addItem(
                    new SideNavItem(translate("persons"), PersonsView.class, LineAwesomeIcon.USERS_SOLID.create()));
        }

        verticalLayout.add(sideNav);

        var locale = UI.getCurrent().getSession().getLocale();
        var languageSwitch = new Button(locale.getLanguage().equals(Locale.ENGLISH.getLanguage()) ? "DE" : "EN");
        languageSwitch.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        languageSwitch.addClickListener(e -> {
            UI.getCurrent()
                .getSession()
                .setLocale(locale.getLanguage().equals(Locale.ENGLISH.getLanguage()) ? Locale.GERMAN : Locale.ENGLISH);
            UI.getCurrent().getPage().reload();
        });

        var languageLayout = new HorizontalLayout(languageSwitch);
        languageLayout.addClassNames(LumoUtility.Margin.SMALL, LumoUtility.Margin.Top.XLARGE);
        verticalLayout.add(languageLayout);

        var version = new Span(applicationVersion);
        version.addClassNames(LumoUtility.Margin.SMALL, LumoUtility.Margin.Top.XLARGE);
        verticalLayout.add(version);

        return verticalLayout;
    }

    private Footer createFooter() {
        var layout = new Footer();

        if (securityContext.isUserLoggedIn()) {
            var userMenu = new MenuBar();
            userMenu.setThemeName("tertiary-inline contrast");

            var userName = userMenu.addItem("");
            var div = new Div();

            div.add(securityContext.getName());
            div.add(new Icon("lumo", "dropdown"));
            div.addClassNames(LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER, LumoUtility.Gap.SMALL);
            userName.add(div);
            var signOut = userName.getSubMenu().addItem("Sign out", e -> authenticationContext.logout());
            signOut.setId("sign-out");

            layout.add(userMenu);
        }
        else {
            var loginLink = new Anchor("login", "Sign in");
            layout.add(loginLink);
        }

        return layout;
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        return MenuConfiguration.getPageHeader(getContent()).orElse("");
    }

}
