package com.metadata.views.actions;

import com.vaadin.guice.annotation.UIScope;
import com.vaadin.ui.Label;

@UIScope
class ActionsLabelTop extends Label{

    ActionsLabelTop(){
        addStyleName("actions-label-top");
        setCaption("Suggested DataTpe Synonyms for 'BERTHE (0.0.1)' and 'Cancer Model (3.1.3)'");
    }
}
