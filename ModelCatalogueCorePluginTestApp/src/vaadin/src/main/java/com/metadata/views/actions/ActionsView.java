package com.metadata.views.actions;

import com.google.inject.Inject;

import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import static com.google.common.base.Strings.isNullOrEmpty;

@GuiceView("batch")
public class ActionsView extends VerticalLayout implements View {

    @Inject
    private ActionDataProvider actionDataProvider;

    @Inject
    ActionsView(ActionsGrid actionsGrid, LabelLayout labelLayout, RunAllPendingActions runAllPendingActions){
        HorizontalLayout topLayout = new HorizontalLayout(labelLayout, runAllPendingActions);
        topLayout.setWidth("100%");
        topLayout.setExpandRatio(labelLayout, 1);

        addComponents(topLayout, actionsGrid);
        setSizeFull();
        setSpacing(true);
        setMargin(true);
        setExpandRatio(actionsGrid, 1);
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {

        final String batchIdString = event.getParameterMap().get("batchId");

        if(!isNullOrEmpty(batchIdString)){
            final int batchId = Integer.parseInt(batchIdString);
            actionDataProvider.setBatchId(batchId);
        }
    }
}
