package com.metadata.components.header;

import com.vaadin.guice.annotation.UIScope;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Notification;

@UIScope
public class ArchiveButton extends HeaderButton {

    ArchiveButton(){
        super(
            VaadinIcons.ARCHIVE,
            "Archive",
            e -> Notification.show("clicked on archive")
        );
    }
}
