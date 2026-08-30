package ch.martinelli.oss.registration.ui.components;

import java.time.format.DateTimeFormatter;

public class DateFormat {

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private DateFormat() {
    }

}
