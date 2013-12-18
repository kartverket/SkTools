package no.statkart.sktools.utils.wsdocgen.processor.xml;

import no.statkart.sktools.utils.wsdocgen.processor.util.*;
import org.w3c.dom.Document;

import javax.annotation.processing.ProcessingEnvironment;
import javax.jws.WebMethod;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.Map;

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

    public void appendService(Element element, String relativeUrl) {
        services.appendChild(buildService(document, element, relativeUrl));
    }

    public Document getDocument() {
        return document;
    }

    org.w3c.dom.Element buildService(Document document, Element element, String relativeUrl) {
        final org.w3c.dom.Element serviceElement = document.createElement("service");
        JavaDocUtils javaDocUtils = findComment(element);

        System.out.println(String.format("Beskrivelse : %s %s", javaDocUtils.getText(), javaDocUtils.getAllTags()));

        serviceElement.setAttribute("name", WSUtils.findWebServiceName(element));
        serviceElement.setAttribute("portName", WSUtils.findWebServicePortTypeName(element));
        serviceElement.setAttribute("namespace", WSUtils.findTargetNamespace(element));
        serviceElement.setAttribute("description", javaDocUtils.getText());
        serviceElement.setAttribute("href", relativeUrl);

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

            System.out.println(String.format("Found method: %s", methodElement));
            //processingEnv.getMessager().printMessage(Diagnostic.Kind.OTHER, String.format("Found method: %s", methodElement));

            JavaDocUtils javaDocUtils = findComment(executableElement);

            method.setAttribute("name", WSUtils.findName(executableElement, usingWebMethodAnnotation));
            method.setAttribute("description", javaDocUtils.getText());

            method.appendChild(buildParameters(document, executableElement, javaDocUtils.getParams()));
            method.appendChild(buildReturns(document, executableElement, javaDocUtils.getReturn()));
            method.appendChild(buildExceptions(document, executableElement, javaDocUtils.getThrows()));
        }
        return method;
    }

    org.w3c.dom.Element buildParameters(Document document, ExecutableElement element, Map<String, String> paramsDocumentation) {
        org.w3c.dom.Element parameters = document.createElement("parameters");
        for (VariableElement variableElement : element.getParameters()) {
            org.w3c.dom.Element parameter = document.createElement("parameter");
            parameter.setAttribute("name", WSUtils.findName(variableElement));
            parameter.setAttribute("description", paramsDocumentation.get(variableElement.getSimpleName().toString()));
            parameter.appendChild(buildType(document, variableElement));
            parameters.appendChild(parameter);
        }
        return parameters;
    }

    org.w3c.dom.Element buildReturns(Document document, ExecutableElement element, String returnDocumentation) {
        org.w3c.dom.Element returns = document.createElement("returns");
        final TypeMirror returnType = element.getReturnType();

        if (!TypeKind.VOID.equals(returnType.getKind())) {
            org.w3c.dom.Element parameter = document.createElement("parameter");
            parameter.setAttribute("name", WSUtils.findName(element));
            if (parameter.getAttribute("name") == null) {
                parameter.setAttribute("name", "return"); //weblogic defaulter til dette navnet?
            }
            parameter.setAttribute("description", returnDocumentation);
            parameter.appendChild(buildType(document, returnType));

            returns.appendChild(parameter);
        }
        return returns;
    }


    org.w3c.dom.Element buildExceptions(Document document, ExecutableElement element, Map<String, String> exceptionsDocumentation) {
        org.w3c.dom.Element exceptions = document.createElement("exceptions");

        for (TypeMirror exceptionType : element.getThrownTypes()) {
            org.w3c.dom.Element exception = document.createElement("exception");
            if (exceptionType instanceof DeclaredType) {
                final Element exceptionElement = ((DeclaredType) exceptionType).asElement();
                exception.setAttribute("name", WSUtils.findName(exceptionElement));
                exception.setAttribute("description", resolveExceptionDocumentation(exceptionsDocumentation, element, exceptionType));

            } else {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, String.format("Unknown exception type: %s", exceptionType));
            }
            exception.appendChild(buildType(document, exceptionType));
            exceptions.appendChild(exception);
        }

        return exceptions;
    }

    private String resolveExceptionDocumentation(Map<String, String> exceptionsDocumentation, ExecutableElement element, TypeMirror exceptionType) {
        String candidate = null;
        for (Map.Entry<String, String> entry : exceptionsDocumentation.entrySet()) {
            if (exceptionType.toString().equals(entry.getKey())) {
                return entry.getValue(); //matcher fqn
            }
            if (exceptionType.toString().endsWith(entry.getKey())) {
                candidate = entry.getKey();
            }
        }

        if (candidate != null) {
            return exceptionsDocumentation.get(candidate);
        } else {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, String.format("Error resolving exception documentation '%s' for element '%s'", exceptionType.toString(), element.getSimpleName()));
            return null;
        }
    }

    org.w3c.dom.Node buildType(Document document, Element element) {
        return new XMLTypeBuilder(document, processingEnv).buildType(element);
    }

    org.w3c.dom.Node buildType(Document document, TypeMirror typeMirror) {
        return new XMLTypeBuilder(document, processingEnv).buildType(typeMirror);
    }


    private JavaDocUtils findComment(Element element) {
        String docComment = processingEnv.getElementUtils().getDocComment(element);
        return JavaDocUtils.parse(docComment);
    }


}
