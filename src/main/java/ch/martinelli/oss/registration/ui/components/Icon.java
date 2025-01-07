package ch.martinelli.oss.registration.ui.components;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.icon.SvgIcon;
import org.vaadin.lineawesome.LineAwesomeIcon;

public class Icon extends SvgIcon {

    public Icon(LineAwesomeIcon lineAwesomeIcon, ComponentEventListener<ClickEvent<SvgIcon>> listener) {
        super(lineAwesomeIcon.create().getSrc());

        getElement()
                .addEventListener("click", event -> listener.onComponentEvent(new ClickEvent<>(this)))
                .addEventData("event.stopPropagation()");
    }
}
