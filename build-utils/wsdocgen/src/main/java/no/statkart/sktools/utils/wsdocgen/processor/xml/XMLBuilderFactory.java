package no.statkart.sktools.utils.wsdocgen.processor.xml;

import org.w3c.dom.Document;

import javax.annotation.processing.ProcessingEnvironment;

/**
 * Factory for xml buildere
 *
 * @author Leif Lislegård
 * @since 1.3
 */
public class XMLBuilderFactory {

    private final org.w3c.dom.Document document;
    private final ProcessingEnvironment processingEnv;

    private XMLServicesBuilder servicesBuilder = null;
    private XMLServiceBuilder serviceBuilder = null;
    private XMLTypeBuilder typeBuilder = null;
    private XMLDescriptionBuilder descriptionBuilder = null;

    public XMLBuilderFactory(Document document, ProcessingEnvironment processingEnv) {
        this.document = document;
        this.processingEnv = processingEnv;
    }


    public XMLServicesBuilder getServicesBuilder() {
        if (servicesBuilder == null) {
            servicesBuilder = new XMLServicesBuilder(this);
        }
        return servicesBuilder;
    }


    public XMLServiceBuilder getServiceBuilder() {
        if (serviceBuilder == null) {
            serviceBuilder = new XMLServiceBuilder(this);
        }
        return serviceBuilder;
    }


    public XMLTypeBuilder getTypeBuilder() {
        if (typeBuilder == null) {
            typeBuilder = new XMLTypeBuilder(this);
        }
        return typeBuilder;
    }

    public XMLDescriptionBuilder getDescriptionBuilder() {
        if (descriptionBuilder == null) {
            descriptionBuilder = new XMLDescriptionBuilder(this);
        }
        return descriptionBuilder;
    }


    public Document getDocument() {
        return document;
    }

    public ProcessingEnvironment getProcessingEnv() {
        return processingEnv;
    }
}
