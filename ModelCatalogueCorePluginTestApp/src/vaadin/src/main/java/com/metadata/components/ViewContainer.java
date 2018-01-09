package com.metadata.components;

import com.vaadin.guice.annotation.UIScope;
import com.vaadin.ui.Panel;

@UIScope
public class ViewContainer extends Panel{
    ViewContainer(){
        addStyleName("view-container");
        setSizeFull();
    }
}
