package no.statkart.sktools.utils.wsdocgen.processor.xml;

import no.statkart.sktools.utils.wsdocgen.processor.util.WSUtils;
import org.w3c.dom.Document;

import javax.annotation.processing.ProcessingEnvironment;
import javax.jws.WebMethod;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

/**
 * Builds XML structure for web services.
 *
 * @author Leif Lislegård
 * @since 1.3 - ny grunnbok sprint 30
 */
public class XMLBuilder {

    private final ProcessingEnvironment processingEnv;
    private final org.w3c.dom.Document document;
    private final org.w3c.dom.Element services;

    public XMLBuilder(Document document, ProcessingEnvironment processingEnv) {
        this.document = document;
        this.processingEnv = processingEnv;

        services = document.createElement("services"); //root node
        document.appendChild(services);
    }

    public void appendService(Element element) {
        services.appendChild(buildService(document, element));
    }



    org.w3c.dom.Element buildService(Document document, Element element) {
        final org.w3c.dom.Element serviceElement = document.createElement("service");

        processingEnv.getMessager().printMessage(Diagnostic.Kind.OTHER, String.format("Beskrivelse : %s", findComment(element)));

        serviceElement.setAttribute("name", WSUtils.findWebServiceName(element));
        serviceElement.setAttribute("namespace", WSUtils.findTargetNamespace(element));
        serviceElement.setAttribute("description", findComment(element));

        serviceElement.appendChild(buildMethods(document, element));

        return serviceElement;
    }

    org.w3c.dom.Element buildMethods(Document document, Element element) {
        org.w3c.dom.Element methods = document.createElement("methods");

        boolean isUsingWebMethodAnnotation = false;
        for (Element enclosedElement : element.getEnclosedElements()) {
            if (enclosedElement.getKind().equals(ElementKind.METHOD) && enclosedElement.getAnnotation(WebMethod.class) != null) {
                isUsingWebMethodAnnotation = true; break;
            }
        }

        for (Element enclosedElement : element.getEnclosedElements()) {
            if (enclosedElement.getKind().equals(ElementKind.METHOD)) {
                org.w3c.dom.Element method = null;

                if (isUsingWebMethodAnnotation) {
                    WebMethod webMethod = enclosedElement.getAnnotation(WebMethod.class);
                    if (webMethod != null && webMethod.exclude() == false) {
                        method = buildMethod(document, enclosedElement, isUsingWebMethodAnnotation);
                    }
                } else {
                    method = buildMethod(document, enclosedElement, isUsingWebMethodAnnotation);
                }

                if (method != null ) {
                    methods.appendChild(method);
                }
            }
        }

        return methods;
    }

    org.w3c.dom.Element buildMethod(Document document, Element methodElement, boolean usingWebMethodAnnotation) {
        org.w3c.dom.Element method = null;
        if (methodElement instanceof ExecutableElement) {
            ExecutableElement executableElement = (ExecutableElement) methodElement;
            method = document.createElement("method");

            processingEnv.getMessager().printMessage(Diagnostic.Kind.OTHER, String.format("Found method: %s", methodElement));

            method.setAttribute("name", WSUtils.findName(executableElement, usingWebMethodAnnotation));
            method.setAttribute("description", findComment(executableElement));

            method.appendChild(buildParameters(document, executableElement));
            method.appendChild(buildReturns(document, executableElement));
            method.appendChild(buildExceptions(document, executableElement));
        }
        return method;
    }

    org.w3c.dom.Element buildParameters(Document document, ExecutableElement element) {
        org.w3c.dom.Element parameters = document.createElement("parameters");
        for (VariableElement variableElement : element.getParameters()) {
            org.w3c.dom.Element parameter = document.createElement("parameter");
            parameter.setAttribute("name", WSUtils.findName(variableElement));
            parameter.setAttribute("descriptions", findComment(variableElement));
            parameter.appendChild(buildType(document, variableElement));
            parameters.appendChild(parameter);
        }
        return parameters;
    }

    org.w3c.dom.Element buildReturns(Document document, ExecutableElement element) {
        org.w3c.dom.Element returns = document.createElement("returns");
        final TypeMirror returnType = element.getReturnType();

        if (!TypeKind.VOID.equals(returnType.getKind())) {
            org.w3c.dom.Element parameter = document.createElement("parameter");
            parameter.setAttribute("name", WSUtils.findName(element));
            if (parameter.getAttribute("name") == null) {
                parameter.setAttribute("name", "return"); //weblogic defaulter til dette navnet?
            }
            parameter.setAttribute("descriptions", ""); //todo
            parameter.appendChild(buildType(document, returnType));

            returns.appendChild(parameter);
        }
        return returns;
    }


    org.w3c.dom.Element buildExceptions(Document document, ExecutableElement element) {
        org.w3c.dom.Element exceptions = document.createElement("exceptions");

        for (TypeMirror exceptionType : element.getThrownTypes()) {
            org.w3c.dom.Element exception = document.createElement("exception");
            if (exceptionType instanceof DeclaredType) {
                final Element exceptionElement = ((DeclaredType) exceptionType).asElement();
                exception.setAttribute("name", WSUtils.findName(exceptionElement));
                exception.setAttribute("descriptions", processingEnv.getElementUtils().getDocComment(exceptionElement));

            } else {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("Unknown exception type: %s", exceptionType));
            }
            exception.appendChild(buildType(document, exceptionType));
            exceptions.appendChild(exception);
        }

        return exceptions;
    }

    org.w3c.dom.Node buildType(Document document, Element element) {
        return new XMLTypeBuilder(document, processingEnv).buildType(element);
    }

    org.w3c.dom.Node buildType(Document document, TypeMirror typeMirror) {
        return new XMLTypeBuilder(document, processingEnv).buildType(typeMirror);
    }


    private String findComment(Element element) {
        String docComment = processingEnv.getElementUtils().getDocComment(element);
        if (docComment == null) {
            docComment = "";
        }
        return docComment.trim();
    }


}
