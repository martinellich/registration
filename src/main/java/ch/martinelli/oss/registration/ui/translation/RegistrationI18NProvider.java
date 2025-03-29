package ch.martinelli.oss.registration.ui.translation;

import com.vaadin.flow.i18n.DefaultI18NProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class RegistrationI18NProvider extends DefaultI18NProvider {

    private final static List<Locale> PROVIDED_LOCALES = List.of(Locale.ENGLISH, Locale.GERMAN);

    public RegistrationI18NProvider() {
        super(PROVIDED_LOCALES);
    }

}
