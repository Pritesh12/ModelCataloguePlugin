package com.metadata.components.header;

import com.vaadin.guice.annotation.UIScope;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Notification;

@UIScope
class ReloadButton extends HeaderButton {

    ReloadButton(){
        super(
            VaadinIcons.REFRESH,
            "reload",
            e -> Notification.show("reload")
        );
    }
}
