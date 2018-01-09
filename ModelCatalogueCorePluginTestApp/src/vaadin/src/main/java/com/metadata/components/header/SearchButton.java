package com.metadata.components.header;

import com.vaadin.guice.annotation.UIScope;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Notification;

@UIScope
public class SearchButton extends HeaderButton {

    SearchButton(){
        super(
            VaadinIcons.SEARCH,
            "",
            e -> Notification.show("clicked on search")
        );
    }
}
