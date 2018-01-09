package com.metadata.views.actions;

import com.google.inject.Inject;

import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.ExternalResource;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.VerticalLayout;

@GuiceView("my_view")
public class MyView extends VerticalLayout implements View {
    @Inject
    MyView() {
        addComponent(new Embedded("", new ExternalResource("www.google.com")));
        setSizeFull();
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
    }
}