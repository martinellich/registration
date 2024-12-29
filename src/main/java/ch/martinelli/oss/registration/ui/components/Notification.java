package ch.martinelli.oss.registration.ui.components;

import com.vaadin.flow.component.notification.NotificationVariant;


public class Notification {

    private Notification() {
    }

    public static void info(String message) {
        com.vaadin.flow.component.notification.Notification notification = createNotification(message);
        notification.open();
    }

    public static void success(String message) {
        com.vaadin.flow.component.notification.Notification notification = createNotification(message);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        notification.open();
    }

    public static void warning(String message) {
        com.vaadin.flow.component.notification.Notification notification = createNotification(message);
        notification.addThemeVariants(NotificationVariant.LUMO_WARNING);
        notification.open();
    }

    public static void error(String message) {
        com.vaadin.flow.component.notification.Notification notification = createNotification(message);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        notification.open();
    }

    private static com.vaadin.flow.component.notification.Notification createNotification(String message) {
        return new com.vaadin.flow.component.notification.Notification(message, 3000, com.vaadin.flow.component.notification.Notification.Position.TOP_END);
    }
}
