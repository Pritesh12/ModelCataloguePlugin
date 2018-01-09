package com.metadata.components;

import com.google.inject.Inject;

import com.metadata.components.header.Header;
import com.vaadin.guice.annotation.UIScope;
import com.vaadin.ui.VerticalLayout;

@UIScope
public class Content extends VerticalLayout {

    @Inject
    Content(Header header, ViewContainer viewContainer){
        setSizeFull();
        addComponents(header, viewContainer);
        setMargin(false);
        setSpacing(false);
        setExpandRatio(viewContainer, 1);
    }
}
