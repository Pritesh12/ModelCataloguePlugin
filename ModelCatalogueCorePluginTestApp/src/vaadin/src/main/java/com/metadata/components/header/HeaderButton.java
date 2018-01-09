package com.metadata.components.header;

import com.vaadin.server.Resource;
import com.vaadin.ui.Button;
import com.vaadin.ui.themes.ValoTheme;

abstract class HeaderButton extends Button{
    HeaderButton(Resource icon, String caption, ClickListener clickListener){
        super(icon);
        setCaption(caption);
        addClickListener(clickListener);
        addStyleName(ValoTheme.BUTTON_BORDERLESS);
    }
}
