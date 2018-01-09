package com.metadata.components.header;

import com.google.inject.Inject;

import com.vaadin.guice.annotation.UIScope;
import com.vaadin.ui.CssLayout;

@UIScope
class HeaderLeft extends CssLayout{
    @Inject
    HeaderLeft(
        LogoButton logoButton,
        ArchiveButton archiveButton,
        RunAllPendingButton runAllPendingButton,
        ReloadButton reloadButton
    ){
        addComponents(logoButton, archiveButton, runAllPendingButton, reloadButton);
    }
}
