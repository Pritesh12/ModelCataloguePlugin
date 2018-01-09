package com.metadata.components.header;

import com.vaadin.guice.annotation.UIScope;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Notification;

@UIScope
class RunAllPendingButton extends HeaderButton {

    RunAllPendingButton(){
        super(
            VaadinIcons.FAST_FORWARD,
            "Run All Pending",
            e -> Notification.show("run all pending")
        );
    }
}
