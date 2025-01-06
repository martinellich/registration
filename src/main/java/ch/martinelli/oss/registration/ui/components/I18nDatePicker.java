package ch.martinelli.oss.registration.ui.components;

import com.vaadin.flow.component.datepicker.DatePicker;

import java.util.Locale;

/**
 * I18nDatePicker is an extension of the standard {@link DatePicker} component, providing internationalization (i18n)
 * support for formatted date display and user interaction based on the user's locale.
 * <p>
 * This class automatically initializes the date picker with a localized configuration using
 * the {@link DatePickerI18nFactory#createDatePickerI18n(Locale)} method. It sets localized month names, weekdays,
 * and other i18n settings based on the current locale.
 */
public class I18nDatePicker extends DatePicker {

    public I18nDatePicker(String label) {
        setLabel(label);

        setLocale(getLocale());

        setI18n(DatePickerI18nFactory.createDatePickerI18n(getLocale()));
    }

}
