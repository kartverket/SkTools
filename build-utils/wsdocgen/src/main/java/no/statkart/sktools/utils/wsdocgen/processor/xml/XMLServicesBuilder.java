package no.statkart.sktools.utils.wsdocgen.processor.xml;


/**
 *
 * @author Leif Lislegård
 * @since 1.3
 */
public class XMLServicesBuilder {

    private final org.w3c.dom.Document document;

    private org.w3c.dom.Element services;



    XMLServicesBuilder(XMLBuilderFactory factory) {
        this.document = factory.getDocument();
    }

    public org.w3c.dom.Element createServices() {
        return appendServicesTo(document);
    }

    public org.w3c.dom.Element appendServicesTo(org.w3c.dom.Node rootNode) {
        services = buildServices();
        rootNode.appendChild(services);
        return services;
    }

    org.w3c.dom.Element buildServices() {
        return document.createElement("services");
    }

}
