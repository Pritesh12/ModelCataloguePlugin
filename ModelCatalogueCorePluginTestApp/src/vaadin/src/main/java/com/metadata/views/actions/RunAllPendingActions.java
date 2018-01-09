package com.metadata.views.actions;


import com.google.inject.Inject;

import com.metadata.service.api.MatchService;
import com.vaadin.guice.annotation.UIScope;
import com.vaadin.ui.Button;

@UIScope
public class RunAllPendingActions extends Button {

    @Inject
    RunAllPendingActions(MatchService matchService, ActionDataProvider actionDataProvider){
        setCaption("run all pending");

        addClickListener(
            new ClickListener() {
                @Override
                public void buttonClick(ClickEvent event) {

                }
            }
        );
    }
}
