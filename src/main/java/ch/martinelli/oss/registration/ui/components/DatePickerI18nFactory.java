package ch.martinelli.oss.registration.ui.components;

import com.vaadin.flow.component.datepicker.DatePicker;

import java.text.DateFormatSymbols;
import java.util.Arrays;
import java.util.Locale;

class DatePickerI18nFactory {

    private DatePickerI18nFactory() {
    }

    /**
     * Creates and configures a {@link DatePicker.DatePickerI18n} instance based on the
     * provided {@link Locale}. This method initializes localized month names, weekdays,
     * short weekdays, and sets the first day of the week.
     * @param locale the {@link Locale} to configure the date picker internationalization
     * settings
     * @return a {@link DatePicker.DatePickerI18n} instance with locale-specific
     * configurations
     */
    static DatePicker.DatePickerI18n createDatePickerI18n(Locale locale) {
        DateFormatSymbols symbols = new DateFormatSymbols(locale);
        DatePicker.DatePickerI18n datePickerI18n = new DatePicker.DatePickerI18n();
        datePickerI18n.setMonthNames(Arrays.asList(symbols.getMonths()));
        datePickerI18n.setFirstDayOfWeek(1);
        datePickerI18n.setWeekdays(Arrays.stream(symbols.getWeekdays()).filter(s -> !s.isEmpty()).toList());
        datePickerI18n.setWeekdaysShort(Arrays.stream(symbols.getShortWeekdays()).filter(s -> !s.isEmpty()).toList());
        return datePickerI18n;
    }

}
