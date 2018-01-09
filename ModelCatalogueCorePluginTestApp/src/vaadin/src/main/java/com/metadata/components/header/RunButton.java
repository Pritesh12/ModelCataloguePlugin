package com.metadata.components.header;

import com.vaadin.guice.annotation.UIScope;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Notification;

@UIScope
public class RunButton extends HeaderButton {

    RunButton(){
        super(
            VaadinIcons.FAST_FORWARD,
            "",
            e -> Notification.show("run all pending")
        );
    }
}
