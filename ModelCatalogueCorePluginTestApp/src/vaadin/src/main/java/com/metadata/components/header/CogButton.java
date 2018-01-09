package com.metadata.components.header;

import com.vaadin.guice.annotation.UIScope;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Notification;

@UIScope
public class CogButton extends HeaderButton {

    CogButton(){
        super(
            VaadinIcons.COG,
            "",
            e -> Notification.show("clicked on cog")
        );
    }
}
