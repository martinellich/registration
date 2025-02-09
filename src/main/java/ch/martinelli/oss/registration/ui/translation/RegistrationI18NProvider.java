package ch.martinelli.oss.registration.ui.translation;

import com.vaadin.flow.i18n.DefaultI18NProvider;

import java.util.List;
import java.util.Locale;

public class RegistrationI18NProvider extends DefaultI18NProvider {

    private final static List<Locale> PROVIDED_LOCALES = List.of(Locale.ENGLISH, Locale.GERMAN);

    public RegistrationI18NProvider(List<Locale> PROVIDED_LOCALES) {
        super(PROVIDED_LOCALES);
    }

    public RegistrationI18NProvider(List<Locale> PROVIDED_LOCALES, ClassLoader classLoader) {
        super(PROVIDED_LOCALES, classLoader);
    }

}
