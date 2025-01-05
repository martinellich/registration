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
import com.vaadin.flow.component.contextmenu.MenuItem;
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
    private final AccessAnnotationChecker accessAnnotationChecker;
    private final String applicationVersion;

    private H1 viewTitle;

    public MainLayout(SecurityContext securityContext, AccessAnnotationChecker accessAnnotationChecker,
                      @Value("${spring.application.version}") String applicationVersion) {
        this.securityContext = securityContext;
        this.accessAnnotationChecker = accessAnnotationChecker;
        this.applicationVersion = applicationVersion;

        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        viewTitle = new H1();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        addToNavbar(true, toggle, viewTitle);
    }

    private void addDrawerContent() {
        Span appName = new Span(translate("application.title"));
        appName.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.LARGE);
        Header header = new Header(appName);

        Scroller scroller = new Scroller(createNavigation());

        addToDrawer(header, scroller, createFooter());
    }

    private VerticalLayout createNavigation() {
        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setPadding(false);
        verticalLayout.setSpacing(false);

        SideNav nav = new SideNav();

        if (accessAnnotationChecker.hasAccess(RegistrationView.class)) {
            nav.addItem(new SideNavItem(translate("registrations"), RegistrationView.class, LineAwesomeIcon.LIST_SOLID.create()));
        }
        if (accessAnnotationChecker.hasAccess(EventRegistrationView.class)) {
            nav.addItem(new SideNavItem(translate("event.registrations"), EventRegistrationView.class, LineAwesomeIcon.TH_LIST_SOLID.create()));
        }
        if (accessAnnotationChecker.hasAccess(RegistrationEmailView.class)) {
            nav.addItem(new SideNavItem(translate("mailing"), RegistrationEmailView.class, LineAwesomeIcon.MAIL_BULK_SOLID.create()));
        }
        if (accessAnnotationChecker.hasAccess(EventsView.class)) {
            nav.addItem(new SideNavItem(translate("events"), EventsView.class, LineAwesomeIcon.CALENDAR_SOLID.create()));
        }
        if (accessAnnotationChecker.hasAccess(PersonsView.class)) {
            nav.addItem(new SideNavItem(translate("persons"), PersonsView.class, LineAwesomeIcon.USERS_SOLID.create()));
        }

        verticalLayout.add(nav);

        Locale locale = UI.getCurrent().getSession().getLocale();
        Button languageSwitch = new Button(locale.equals(Locale.ENGLISH) ? "DE" : "EN");
        languageSwitch.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        languageSwitch.addClickListener(e -> {
            UI.getCurrent().getSession().setLocale(locale.equals(Locale.ENGLISH) ? Locale.GERMAN : Locale.ENGLISH);
            UI.getCurrent().getPage().reload();
        });

        HorizontalLayout languageLayout = new HorizontalLayout(languageSwitch);
        languageLayout.addClassNames(LumoUtility.Margin.SMALL, LumoUtility.Margin.Top.XLARGE);
        verticalLayout.add(languageLayout);

        Span version = new Span(applicationVersion);
        version.addClassNames(LumoUtility.Margin.SMALL, LumoUtility.Margin.Top.XLARGE);
        verticalLayout.add(version);

        return verticalLayout;
    }

    private Footer createFooter() {
        Footer layout = new Footer();

        if (securityContext.isUserLoggedIn()) {
            MenuBar userMenu = new MenuBar();
            userMenu.setThemeName("tertiary-inline contrast");

            MenuItem userName = userMenu.addItem("");
            Div div = new Div();
            div.add(securityContext.getUsername());
            div.add(new Icon("lumo", "dropdown"));
            div.addClassNames(LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER, LumoUtility.Gap.SMALL);
            userName.add(div);
            MenuItem signOut = userName.getSubMenu().addItem("Sign out", e -> securityContext.logout());
            signOut.setId("sign-out");

            layout.add(userMenu);
        } else {
            Anchor loginLink = new Anchor("login", "Sign in");
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
