package com.metadata;

import com.metadata.components.Content;
import com.metadata.components.ViewContainer;
import com.vaadin.annotations.Theme;
import com.vaadin.guice.annotation.GuiceUI;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

@Theme("metadata-theme")
@GuiceUI(content = Content.class, viewContainer = ViewContainer.class)
public class MetaDataUi extends UI {

    @Override
    protected void init(VaadinRequest vaadinRequest) {
    }

}
