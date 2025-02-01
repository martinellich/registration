package ch.martinelli.oss.registration.ui.components;

import com.vaadin.flow.component.notification.NotificationVariant;

public class Notification {

    private Notification() {
    }

    public static void success(String message) {
        var notification = createNotification(message);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        notification.open();
    }

    public static void error(String message) {
        var notification = createNotification(message);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        notification.open();
    }

    private static com.vaadin.flow.component.notification.Notification createNotification(String message) {
        return new com.vaadin.flow.component.notification.Notification(message, 3000,
                com.vaadin.flow.component.notification.Notification.Position.TOP_END);
    }

}
