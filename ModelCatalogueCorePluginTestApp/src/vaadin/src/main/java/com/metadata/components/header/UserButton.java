package com.metadata.components.header;

import com.vaadin.guice.annotation.UIScope;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Notification;

@UIScope
public class UserButton extends HeaderButton {

    UserButton(){
        super(
            VaadinIcons.USER,
            "",
            e -> Notification.show("clicked on user")
        );
    }
}
