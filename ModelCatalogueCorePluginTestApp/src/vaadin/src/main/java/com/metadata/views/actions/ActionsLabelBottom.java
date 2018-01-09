package com.metadata.views.actions;

import com.vaadin.guice.annotation.UIScope;
import com.vaadin.ui.Label;

@UIScope
class ActionsLabelBottom extends Label{

    ActionsLabelBottom(){
        addStyleName("actions-label-bottom");
        setCaption("(created 28/11/2017, last updated 28/11/2017 11:5)");
    }
}
