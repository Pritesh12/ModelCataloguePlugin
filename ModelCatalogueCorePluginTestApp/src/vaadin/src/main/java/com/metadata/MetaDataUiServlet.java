package com.metadata;

import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.guice.annotation.PackagesToScan;
import com.vaadin.guice.server.GuiceVaadinServlet;

import javax.servlet.annotation.WebServlet;

@PackagesToScan("com.metadata")
@WebServlet(urlPatterns = "/*", name = "MetaDataUiServlet", asyncSupported = true)
@VaadinServletConfiguration(ui = MetaDataUi.class, productionMode = false)
public class MetaDataUiServlet extends GuiceVaadinServlet {
}
