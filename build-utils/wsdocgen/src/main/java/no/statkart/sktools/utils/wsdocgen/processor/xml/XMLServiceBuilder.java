package no.statkart.sktools.utils.wsdocgen.processor.xml;

import no.statkart.sktools.utils.wsdocgen.processor.util.JavaDocUtils;
import no.statkart.sktools.utils.wsdocgen.processor.util.WSUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.annotation.processing.ProcessingEnvironment;
import javax.jws.WebMethod;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds {@code <service>} elements for parametrized class
 *
 * @author Leif Lislegård
 * @since 1.3
 */
public class XMLServiceBuilder {

    private final XMLBuilderFactory factory;
    private final ProcessingEnvironment processingEnv;
    private final org.w3c.dom.Document document;


    XMLServiceBuilder(XMLBuilderFactory factory) {
        this.factory = factory;
        this.processingEnv = factory.getProcessingEnv();
        this.document = factory.getDocument();
    }


    public org.w3c.dom.Element appendServiceTo(Node servicesNode, Element element, String relativeUrl, Element wsiElement) {
        org.w3c.dom.Element service = buildService(document, element, relativeUrl, wsiElement);
        servicesNode.appendChild(service);
        return service;
    }

    org.w3c.dom.Element buildService(Document document, Element element, String relativeUrl, Element wsiElement) {
        final org.w3c.dom.Element serviceElement = document.createElement("service");
        JavaDocUtils javaDocUtils = findComment(element, wsiElement);

        System.out.println(String.format("Beskrivelse : %s %s", javaDocUtils.getText(), javaDocUtils.getAllTags()));

        serviceElement.setAttribute("name", WSUtils.findWebServiceName(element));
        serviceElement.setAttribute("portName", WSUtils.findWebServicePortTypeName(element));
        serviceElement.setAttribute("namespace", WSUtils.findTargetNamespace(element));
        serviceElement.setAttribute("href", relativeUrl);

        serviceElement.appendChild(factory.getDescriptionBuilder().buildDescription(javaDocUtils));
        serviceElement.appendChild(buildMethods(document, element, wsiElement));

        return serviceElement;
    }


    private JavaDocUtils findComment(Element element, Element wsiElement) {
        String docComment = processingEnv.getElementUtils().getDocComment(element);

        if(docComment == null && wsiElement != null) {
            docComment = processingEnv.getElementUtils().getDocComment(wsiElement);
        }

        return JavaDocUtils.parse(docComment);
    }

    org.w3c.dom.Element buildMethods(Document document, Element element, Element wsiElement) {
        org.w3c.dom.Element methods = document.createElement("methods");

        boolean isUsingWebMethodAnnotation = false;
        for (Element enclosedElement : element.getEnclosedElements()) {
            if (enclosedElement.getKind().equals(ElementKind.METHOD) && enclosedElement.getAnnotation(WebMethod.class) != null) {
                isUsingWebMethodAnnotation = true;
                break;
            }
        }

        Map<String, Element> wsiEnclosedElements = new HashMap<>();
        if (wsiElement != null) {
            for (Element element1 : wsiElement.getEnclosedElements()) {
                wsiEnclosedElements.put(element1.getSimpleName().toString(), element1);
            }
        }

        for (Element enclosedElement : element.getEnclosedElements()) {

            if (enclosedElement.getKind().equals(ElementKind.METHOD)) {
                org.w3c.dom.Element method = null;

                if (isUsingWebMethodAnnotation) {
                    WebMethod webMethod = enclosedElement.getAnnotation(WebMethod.class);
                    if (webMethod != null && webMethod.exclude() == false) {
                        method = buildMethod(document, enclosedElement, isUsingWebMethodAnnotation, wsiEnclosedElements.get(enclosedElement.getSimpleName().toString()));
                    }
                } else {
                    method = buildMethod(document, enclosedElement, isUsingWebMethodAnnotation, wsiEnclosedElements.get(enclosedElement.getSimpleName().toString()));
                }

                if (method != null) {
                    methods.appendChild(method);
                }
            }
        }

        return methods;
    }


    org.w3c.dom.Element buildMethod(Document document, Element methodElement, boolean usingWebMethodAnnotation, Element wsiElement) {
        org.w3c.dom.Element method = null;
        if (methodElement instanceof ExecutableElement) {
            ExecutableElement executableElement = (ExecutableElement) methodElement;
            method = document.createElement("method");

            System.out.println(String.format("Found method: %s", methodElement));
            //processingEnv.getMessager().printMessage(Diagnostic.Kind.OTHER, String.format("Found method: %s", methodElement));

            JavaDocUtils javaDocUtils = findComment(executableElement, wsiElement);

            method.setAttribute("name", WSUtils.findMethodName(executableElement, usingWebMethodAnnotation));

            method.appendChild(factory.getDescriptionBuilder().buildDescription(javaDocUtils));
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
            parameter.setAttribute("name", WSUtils.nameForParameter(variableElement));

            String documentationString = paramsDocumentation.get(variableElement.getSimpleName().toString());
            parameter.appendChild(factory.getDescriptionBuilder().buildDescription(documentationString));
            parameter.setAttribute("description", documentationString); //no-escaped text not possible with attribute...

            parameter.appendChild(factory.getTypeBuilder().buildType(variableElement.asType()));

            parameters.appendChild(parameter);
        }
        return parameters;
    }

    org.w3c.dom.Element buildReturns(Document document, ExecutableElement element, String returnDocumentation) {
        org.w3c.dom.Element returns = document.createElement("returns");
        final TypeMirror returnType = element.getReturnType();

        if (!TypeKind.VOID.equals(returnType.getKind())) {
            final org.w3c.dom.Element parameter = document.createElement("parameter");
            parameter.setAttribute("name", WSUtils.nameForReturn(element));
            parameter.appendChild(factory.getDescriptionBuilder().buildDescription(returnDocumentation));
            parameter.appendChild(factory.getTypeBuilder().buildType(returnType));

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
                exception.setAttribute("name", WSUtils.nameForException(exceptionElement));
                exception.appendChild(factory.getDescriptionBuilder().buildDescription(resolveExceptionDocumentation(exceptionsDocumentation, element, exceptionType)));

            } else {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, String.format("Unknown exception type: %s", exceptionType));
            }
            exception.appendChild(factory.getTypeBuilder().buildType(exceptionType));

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
}
