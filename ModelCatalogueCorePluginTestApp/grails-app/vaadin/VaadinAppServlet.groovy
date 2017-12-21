@com.vaadin.guice.annotation.PackagesToScan("com.metadata")
@javax.servlet.annotation.WebServlet(urlPatterns = "/vaadin/*", name = "MetaDataUiServlet", asyncSupported = true)
@com.vaadin.annotations.VaadinServletConfiguration(ui = com.metadata.MetaDataUi.class, productionMode = false)
class VaadinAppServlet extends com.metadata.MetaDataUi.MetaDataUiServlet {
}

