package com.metadata.components.header;

import com.google.inject.Inject;

import com.vaadin.guice.annotation.UIScope;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

@UIScope
public class Header extends HorizontalLayout{
    @Inject
    Header(HeaderLeft headerLeft, HeaderRight headerRight){
        Label spacer = new Label();
        addComponents(headerLeft, spacer, headerRight);
        setExpandRatio(spacer, 1);
        setWidth("100%");
    }
}
