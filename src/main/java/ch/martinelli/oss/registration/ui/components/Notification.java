package ch.martinelli.oss.registration.ui.components;

public class Notification extends com.vaadin.flow.component.notification.Notification {

    public static com.vaadin.flow.component.notification.Notification show(String text) {
        return show(text, 5000, Position.TOP_END);
    }
}
