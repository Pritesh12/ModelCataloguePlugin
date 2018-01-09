package com.metadata.components.header;

import com.google.inject.Inject;

import com.vaadin.guice.annotation.UIScope;
import com.vaadin.ui.CssLayout;

@UIScope
class HeaderRight extends CssLayout{
    @Inject
    HeaderRight(
        SearchButton searchButton,
        RunButton runButton,
        CogButton cogButton,
        UserButton userButton
    ){
        addComponents(searchButton, runButton, cogButton, userButton);
    }
}
