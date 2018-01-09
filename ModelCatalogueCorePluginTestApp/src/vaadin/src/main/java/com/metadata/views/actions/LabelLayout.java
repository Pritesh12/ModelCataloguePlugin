package com.metadata.views.actions;

import com.google.inject.Inject;

import com.vaadin.guice.annotation.UIScope;
import com.vaadin.ui.CssLayout;

@UIScope
class LabelLayout extends CssLayout {

    @Inject
    LabelLayout(ActionsLabelTop actionsLabelTop, ActionsLabelBottom actionsLabelBottom){
        addComponents(actionsLabelTop, actionsLabelBottom);
        addStyleName("label-layout");
    }
}
