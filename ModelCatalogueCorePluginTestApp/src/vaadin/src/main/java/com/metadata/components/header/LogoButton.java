package com.metadata.components.header;

import com.vaadin.guice.annotation.UIScope;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Notification;

@UIScope
class LogoButton extends HeaderButton {

    LogoButton(){
        super(
            VaadinIcons.BOOK,
            "Metadata Exchange",
            e -> Notification.show("clicked on logo")
        );
    }
}
